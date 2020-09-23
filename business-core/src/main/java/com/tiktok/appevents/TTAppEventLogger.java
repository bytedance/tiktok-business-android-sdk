package com.tiktok.appevents;

import android.content.pm.PackageInfo;

import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTAppEventLogger {
    static final String TAG = TTAppEventLogger.class.getName();

    final boolean lifecycleTrackEnable;
    final boolean advertiserIDCollectionEnable;

    TTLogger logger;
    TTKeyValueStore store;
    PackageInfo packageInfo;
    Lifecycle lifecycle;
    ExecutorService executor;
    TTIdentifierFactory.AdInfo adInfo;
    boolean adInfoRun = false;

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
        executor = Executors.newSingleThreadExecutor();

        /* ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacks activityLifecycleCallbacks = new TTActivityLifecycleCallbacks(this);
        TiktokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        this.runIdentifierFactory();

        String userAgent = new WebView(TiktokBusinessSdk.getApplicationContext()).getSettings().getUserAgentString();
        logger.verbose(userAgent);
    }

    public void track(@NonNull String event, @Nullable TTProperty props) {
        if (props == null) props = new TTProperty();
        TTProperty finalProps = props;
        executor.execute(() -> {
            logger.debug(event + " : " + finalProps.get().toString());
            // call save to file interface
        });
    }

    public void flush() {
        logger.verbose("flush called");
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
    }
}
