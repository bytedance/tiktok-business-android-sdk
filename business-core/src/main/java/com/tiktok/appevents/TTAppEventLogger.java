package com.tiktok.appevents;

import android.app.Application;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTAppEventLogger {
    static final String TAG = TTAppEventLogger.class.getName();

    final TiktokBusinessSdk ttSdk;
    final Application application;
    final String appKey;
    final TiktokBusinessSdk.LogLevel logLevel;
    final boolean lifecycleTrackEnable;
    final boolean advertiserIDCollectionEnable;

    TTLogger logger;
    TTKeyValueStore store;
    PackageInfo packageInfo;
    Lifecycle lifecycle;
    ExecutorService executor;
    TTIdentifierFactory.AdInfo adInfo;
    boolean adInfoRun = false;
    Queue<EventLog> eventLogQueue;

    public TTAppEventLogger(TiktokBusinessSdk ttSdk,
                            Application application,
                            String appKey,
                            TiktokBusinessSdk.LogLevel logLevel,
                            boolean lifecycleTrackEnable,
                            boolean advertiserIDCollectionEnable) {
        this.ttSdk = ttSdk;
        this.appKey = appKey;
        this.application = application;
        this.logLevel = logLevel;
        logger = new TTLogger(TAG, logLevel);
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.advertiserIDCollectionEnable = advertiserIDCollectionEnable;
        this.eventLogQueue = new LinkedList<>();
        /* SharedPreferences helper */
        store = new TTKeyValueStore(application.getApplicationContext());
        try {
            packageInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
        } catch (Exception ignored) {}

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();
        executor = Executors.newSingleThreadExecutor();

        /* ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacks activityLifecycleCallbacks = new TTActivityLifecycleCallbacks(this);
        this.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        this.runIdentifierFactory();
    }

    public void track(@NonNull String event, @Nullable TTProperty props) {
        if (props == null) props = new TTProperty();
        TTProperty finalProps = props;
        executor.execute(() -> {
            logger.debug(event + " : " + finalProps.get().toString());
            eventLogQueue.add(new EventLog(event, finalProps));
            executeQueue();
        });
    }

    public void flush() {
        logger.verbose("flush called");
    }

    private void runIdentifierFactory() {
        TTIdentifierFactory.getAdvertisingId(application, logLevel, new TTIdentifierFactory.Listener() {
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

    static class EventLog {
        String eventType;
        TTProperty property;

        EventLog(@NonNull String et, @Nullable TTProperty props) {
            this.eventType = et;
            this.property = props;
        }
    }

    private boolean loggerInitialized() {
        return this.adInfoRun;
    }

    private void executeQueue() {
        if (!loggerInitialized()) return;
        logger.verbose("queue started");
    }
}
