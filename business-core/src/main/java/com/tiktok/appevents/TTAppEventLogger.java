package com.tiktok.appevents;

import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.model.TTAppEvent;
import com.tiktok.model.TTRequest;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TTAppEventLogger {
    static final String TAG = TTAppEventLogger.class.getName();

    private static final int TIME_BUFFER = 15;
    private static final int THRESHOLD = 100;

    final boolean lifecycleTrackEnable;
    final boolean advertiserIDCollectionEnable;

    TTLogger logger;
    TTKeyValueStore store;
    PackageInfo packageInfo;
    Lifecycle lifecycle;
    TTIdentifierFactory.AdInfo adInfo;
    boolean adInfoRun = false;

    int flushId = 0;

    ScheduledExecutorService eventLoop;

    ScheduledFuture<?> flushFuture = null;

    private Runnable batchFlush = () -> {
        flushFuture = null;
        flush(FlushReason.TIMER);
    };

    public TTAppEventLogger(TiktokBusinessSdk ttSdk,
                            boolean lifecycleTrackEnable,
                            boolean advertiserIDCollectionEnable) {
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.advertiserIDCollectionEnable = advertiserIDCollectionEnable;
        /* SharedPreferences helper */
        store = new TTKeyValueStore(TiktokBusinessSdk.getApplicationContext());
        try {
            packageInfo = TiktokBusinessSdk.getApplicationContext().getPackageManager()
                    .getPackageInfo(TiktokBusinessSdk.getApplicationContext().getPackageName(), 0);
        } catch (Exception ignored) {}

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();
        eventLoop = Executors.newSingleThreadScheduledExecutor();
        /* ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacks activityLifecycleCallbacks = new TTActivityLifecycleCallbacks(this);
        TiktokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        this.runIdentifierFactory();
    }

    public void track(@NonNull String event, @Nullable TTProperty props) {
        if (props == null) props = new TTProperty();
        TTProperty finalProps = props;
        eventLoop.execute(() -> {
            logger.debug(event + " : " + finalProps.get().toString());

            TTAppEventsQueue.addEvent(new TTAppEvent(event, finalProps.get().toString()));

            if (TTAppEventsQueue.size() > THRESHOLD) {
                flush(FlushReason.THRESHOLD);
            } else if (flushFuture == null) {
                flushFuture = eventLoop.schedule(batchFlush, TIME_BUFFER, TimeUnit.SECONDS);
            }
        });
    }

    public void flush() {
        logger.verbose("FORCE_FLUSH called");
        flush(FlushReason.FORCE_FLUSH);
    }

    private void runIdentifierFactory() {
        TTIdentifierFactory.getAdvertisingId(
                TiktokBusinessSdk.getApplicationContext(), TiktokBusinessSdk.getLogLevel(),
                new TTIdentifierFactory.Listener() {
            @Override
            public void onIdentifierFactoryFinish(TTIdentifierFactory.AdInfo ad) {
                adInfoRun = true;
                adInfo = ad;
                executeQueue();
            }

            @Override
            public void onIdentifierFactoryFail(Exception e) {
                adInfoRun = true;
                adInfo = null;
                logger.error(e, "unable to fetch Advertising Id");
                executeQueue();
            }
        });
    }

    String getVersionName() {
        return packageInfo.versionName;
    }

    long getVersionCode() {
        return PackageInfoCompat.getLongVersionCode(packageInfo);
    }

    private boolean loggerInitialized() {
        return this.adInfoRun;
    }

    private void executeQueue() {
        if (!loggerInitialized()) return;
        logger.verbose("called after prefetch & async tasks. Run the first batch from disk if any");
        flush(FlushReason.START_UP);
    }

    private void flush(FlushReason reason) {

        if (!loggerInitialized()) return;

        logger.verbose("Start flush, version %d reason is %s", flushId, reason.name());

        TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

        appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

        String appId = TiktokBusinessSdk.getApplicationContext().getPackageName();

        List<TTAppEvent> eventList = TTRequest.appEventReport(appEventPersist.getAppEvents(), appId, "123456");

        if (!eventList.isEmpty()){//flush failed, persist events
            TTAppEventStorage.persist(eventList);
        }

        logger.verbose("END flush, version %d reason is %s", flushId, reason.name());

        flushId++;
    }

    enum FlushReason {
        THRESHOLD, // when reaching the threshold of the event queue
        TIMER, // triggered every 15 seconds
        START_UP, // when app is started, flush all the accumulated events
        FORCE_FLUSH, // when developer calls flush from app
    }
}
