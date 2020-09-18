package com.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.appevents.TTAppEventLogger;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.appevents.TTProperty;
import com.tiktok.util.TTLogger;

import java.util.HashMap;

import static com.tiktok.util.TTConst.TTSDK_CONFIG_ADVID;
import static com.tiktok.util.TTConst.TTSDK_CONFIG_APPKEY;
import static com.tiktok.util.TTConst.TTSDK_CONFIG_DEBUG;
import static com.tiktok.util.TTConst.TTSDK_CONFIG_LIFECYCLE;

public class TiktokBusinessSdk {
    static final String TAG = TiktokBusinessSdk.class.getName();

    static volatile TiktokBusinessSdk ttSdk = null;
    static TTAppEventLogger appEventLogger;

    final Application application;
    final String appKey;
    final LogLevel logLevel;

    TTLogger logger;

    private TiktokBusinessSdk(TTConfig ttConfig) {
        /* no write key exception */
        if (ttConfig.appKey == null) throw new IllegalArgumentException("app key not found");
        appKey = ttConfig.appKey;
        /* validation done in TTConfig */
        application = ttConfig.application;
        /* sdk logger & loglevel */
        if (ttConfig.debug) {
            logLevel = LogLevel.VERBOSE;
        } else {
            logLevel = LogLevel.INFO;
        }
        logger = new TTLogger(TAG, logLevel);
    }

    public static synchronized void startTracking(TTConfig ttConfig) {
        if (ttSdk != null) throw new RuntimeException("TiktokSdk instance already exists");
        ttSdk = new TiktokBusinessSdk(ttConfig);
        storeConfig(ttConfig);
        appEventLogger = new TTAppEventLogger(ttSdk,
                ttSdk.application,
                ttSdk.appKey,
                ttSdk.logLevel,
                ttConfig.lifecycleTrackEnable,
                ttConfig.advertiserIDCollectionEnable);
    }

    public void track(@NonNull String event) {
        appEventLogger.track(event);
    }

    public void track(@NonNull String event, @Nullable TTProperty props) {
        appEventLogger.track(event, props);
    }

    public static TiktokBusinessSdk with(Context context) {
        if (ttSdk == null) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            synchronized (TiktokBusinessSdk.class) {
                if (ttSdk == null) {
                    startTracking(rebuildConfig(context));
                }
            }
        }
        return ttSdk;
    }

    static void storeConfig(TTConfig ttConfig) {
        TTKeyValueStore store = new TTKeyValueStore(ttConfig.application.getApplicationContext());
        HashMap<String, Object> data = new HashMap<>();
        data.put(TTSDK_CONFIG_APPKEY, ttConfig.appKey);
        data.put(TTSDK_CONFIG_DEBUG, ttConfig.debug);
        data.put(TTSDK_CONFIG_LIFECYCLE, ttConfig.lifecycleTrackEnable);
        data.put(TTSDK_CONFIG_ADVID, ttConfig.advertiserIDCollectionEnable);
        store.set(data);
    }

    static TTConfig rebuildConfig(Context ctx) {
        TTKeyValueStore store = new TTKeyValueStore(ctx);
        TTConfig ttConfig = new TTConfig(ctx)
                .setAppKey(store.get(TTSDK_CONFIG_APPKEY));
        if (store.get(TTSDK_CONFIG_DEBUG).equals("true")) {
            ttConfig.enableDebug();
        }
        if (store.get(TTSDK_CONFIG_LIFECYCLE).equals("false")) {
            ttConfig.optOutAutoEventTracking();
        }
        if (store.get(TTSDK_CONFIG_ADVID).equals("false")) {
            ttConfig.optOutAdvertiserIDCollection();
        }
        return ttConfig;
    }

    public static class TTConfig {
        private final Application application;
        private String appKey;
        private boolean debug = false;
        private boolean lifecycleTrackEnable = true;
        private boolean advertiserIDCollectionEnable = true;

        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();

            /* try fetch app key from meta data */
            try {
                ApplicationInfo appInfo = application.getPackageManager().getApplicationInfo(
                        application.getPackageName(), PackageManager.GET_META_DATA);
                Object key = appInfo.metaData.get("com.tiktok.sdk.AppKey");
                if (key instanceof String) {
                    appKey = key.toString();
                }
            } catch (Exception ignored) {}
        }

        public TTConfig enableDebug() {
            debug = true;
            return this;
        }

        public TTConfig setAppKey(String key) {
            appKey = key;
            return this;
        }

        public TTConfig optOutAutoEventTracking() {
            lifecycleTrackEnable = false;
            return this;
        }

        public TTConfig optOutAdvertiserIDCollection() {
            advertiserIDCollectionEnable = false;
            return this;
        }
    }

    /** Controls the level of logging. */
    public enum LogLevel {
        /** No logging. */
        NONE,
        /** Log exceptions only. */
        INFO,
        /** Log exceptions and print debug output. */
        DEBUG,
        /** Same as {@link LogLevel#DEBUG}, and log transformations in bundled integrations. */
        VERBOSE;
        public boolean log() {
            return this != NONE;
        }
    }

}
