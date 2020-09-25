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

    /** Singleton instance for {@link TiktokBusinessSdk} */
    static volatile TiktokBusinessSdk ttSdk = null;
    /** {@link TTAppEventLogger} package singleton */
    static TTAppEventLogger appEventLogger;

    /** app {@link Context} */
    private static Application applicationContext;
    /** access token */
    private static String accessToken;
    /** {@link LogLevel} of initialized sdk */
    private static LogLevel logLevel;

    /** logger util */
    TTLogger logger;

    private TiktokBusinessSdk(TTConfig ttConfig) {
        /* no write key exception */
        if (ttConfig.accessToken == null) throw new IllegalArgumentException("access token not found");
        accessToken = ttConfig.accessToken;
        /* validation done in TTConfig */
        applicationContext = ttConfig.application;
        /* sdk logger & loglevel */
        if (ttConfig.debug) {
            logLevel = LogLevel.VERBOSE;
        } else {
            logLevel = LogLevel.INFO;
        }
        logger = new TTLogger(TAG, logLevel);
    }

    public static synchronized void startTracking(TTConfig ttConfig) {
        if (ttSdk != null) throw new RuntimeException("TiktokBusinessSdk instance already exists");
        ttSdk = new TiktokBusinessSdk(ttConfig);
        storeConfig(ttConfig);
        appEventLogger = new TTAppEventLogger(ttSdk,
                ttConfig.lifecycleTrackEnable,
                ttConfig.advertiserIDCollectionEnable);
    }

    /** public interface for tracking Event without custom properties */
    public static void trackEvent(@NonNull String event) {
        appEventLogger.track(event, null);
    }

    /** public interface for tracking Event with custom properties */
    public static void trackEvent(@NonNull String event, @Nullable TTProperty props) {
        appEventLogger.track(event, props);
    }

    public void flush() {
        appEventLogger.flush();
    }

    /** applicationContext getter */
    public static Application getApplicationContext() {
        // TODO: 22/09/20 validate if sdk initialised
        return applicationContext;
    }

    /** appKey getter */
    public static String getAccessToken() {
        return accessToken;
    }

    /** logLevel getter */
    public static LogLevel getLogLevel() {
        return logLevel;
    }

    /** stores the config in SharedPreferences */
    static void storeConfig(TTConfig ttConfig) {
        TTKeyValueStore store = new TTKeyValueStore(ttConfig.application.getApplicationContext());
        HashMap<String, Object> data = new HashMap<>();
        data.put(TTSDK_CONFIG_APPKEY, ttConfig.accessToken);
        data.put(TTSDK_CONFIG_DEBUG, ttConfig.debug);
        data.put(TTSDK_CONFIG_LIFECYCLE, ttConfig.lifecycleTrackEnable);
        data.put(TTSDK_CONFIG_ADVID, ttConfig.advertiserIDCollectionEnable);
        store.set(data);
    }

    /** rebuilds TTConfig obj from SharedPreferences */
    static TTConfig rebuildConfig(Context ctx) {
        TTKeyValueStore store = new TTKeyValueStore(ctx);
        TTConfig ttConfig = new TTConfig(ctx)
                .setAccessToken(store.get(TTSDK_CONFIG_APPKEY));
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

    /** To get config and permissions from the app */
    public static class TTConfig {
        /** application context */
        private final Application application;
        /** Access-Token for api calls */
        private String accessToken;
        /** to enable logs */
        private boolean debug = false;
        /** to enable auto event tracking */
        private boolean lifecycleTrackEnable = true;
        /** confirmation to read gaid */
        private boolean advertiserIDCollectionEnable = true;

        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();

            /** try fetch app key from AndroidManifest file first */
            try {
                ApplicationInfo appInfo = application.getPackageManager().getApplicationInfo(
                        application.getPackageName(), PackageManager.GET_META_DATA);
                Object key = appInfo.metaData.get("com.tiktok.sdk.AccessToken");
                if (key instanceof String) {
                    accessToken = key.toString();
                }
            } catch (Exception ignored) {}
        }

        /** Enables debug logs */
        public TTConfig enableDebug() {
            debug = true;
            return this;
        }

        /** to set the access token if not in manifest file */
        public TTConfig setAccessToken(String key) {
            accessToken = key;
            return this;
        }

        /** to disable auto event tracking & lifecycle listeners */
        public TTConfig optOutAutoEventTracking() {
            lifecycleTrackEnable = false;
            return this;
        }

        /** to disable gaid in tracking */
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
