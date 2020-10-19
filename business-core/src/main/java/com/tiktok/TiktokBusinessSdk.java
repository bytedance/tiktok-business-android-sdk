package com.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.appevents.TTAppEventLogger;
import com.tiktok.appevents.TTCrashHandler;
import com.tiktok.appevents.TTProperty;
import com.tiktok.util.TTLogger;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class TiktokBusinessSdk {
    static final String TAG = TiktokBusinessSdk.class.getName();

    /**
     * Singleton instance for {@link TiktokBusinessSdk}
     */
    static volatile TiktokBusinessSdk ttSdk = null;
    /**
     * {@link TTAppEventLogger} package singleton
     */
    static TTAppEventLogger appEventLogger;

    /**
     * app {@link Context}
     */
    private static Application applicationContext;

    private static boolean gaidCollectionEnabled = true;
    /**
     * app_id
     */
    private static String appId;
    /**
     * access token
     */
    private static String accessToken;
    /**
     * {@link LogLevel} of initialized sdk
     */
    private static LogLevel logLevel = LogLevel.INFO;
    /**
     * if set to false, only save to memory and disk, no api request will be sent
     */
    private static AtomicBoolean networkSwitch;

    /**
     * logger util
     */
    TTLogger logger;

    private TiktokBusinessSdk(TTConfig ttConfig) {
        /* no app id exception */
        if (ttConfig.appId == null) throw new IllegalArgumentException("app id not found");
        appId = ttConfig.appId;
        /* no write key exception */
        if (ttConfig.accessToken == null)
            throw new IllegalArgumentException("access token not found");
        accessToken = ttConfig.accessToken;
        /* validation done in TTConfig */
        applicationContext = ttConfig.application;
        gaidCollectionEnabled = ttConfig.advertiserIDCollectionEnable;
        /* sdk logger & loglevel */
        if (ttConfig.debug) {
            logLevel = LogLevel.VERBOSE;
        } else {
            logLevel = LogLevel.INFO;
        }
        logger = new TTLogger(TAG, logLevel);
        networkSwitch = new AtomicBoolean(ttConfig.autoStart);
    }

    /**
     * initializeSdk
     */
    public static synchronized void initializeSdk(TTConfig ttConfig) {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> TTCrashHandler.handleCrash(TAG, e));
        if (ttSdk != null) throw new RuntimeException("TiktokBusinessSdk instance already exists");
        ttSdk = new TiktokBusinessSdk(ttConfig);
        appEventLogger = new TTAppEventLogger(ttSdk,
                ttConfig.autoEvent);
    }

    /**
     * startTracking if turnOffAutoTracking enabled
     */
    public static void startTrack() {
        if (!networkSwitch.get()) {
            networkSwitch.set(true);
            appEventLogger.forceFlush();
        }
    }

    public static synchronized void setUpSdkListeners(
            MemoryListener ml,
            DiskStatusListener dl,
            NetworkListener nl,
            NextTimeFlushListener nfl
    ) {
        if (ml != null) {
            memoryListener = ml;
        }
        if (dl != null) {
            diskListener = dl;
        }
        if (nl != null) {
            networkListener = nl;
        }
        if (nfl != null) {
            nextTimeFlushListener = nfl;
        }
    }

    // inner status listeners, for debugging purpose
    public interface DiskStatusListener {
        public void onDiskChange(int diskSize, boolean read);

        public void onDumped(int dumped);
    }

    public interface NextTimeFlushListener {
        // how many seconds until next auto flush
        public void timeLeft(int timeLeft);

        // how many until threshold
        // i.e. threshold is 100, current in memory is 80, then left will be 100 - 80 = 20
        public void thresholdLeft(int threshold, int left);
    }

    public interface MemoryListener {
        void onMemoryChange(int size);
    }

    public interface NetworkListener {
        void onNetworkChange(int toBeSentRequests, int successfulRequest, int failedRequests,
                             int totalRequests, int totalSuccessRequests);
    }

    public static DiskStatusListener diskListener;
    public static MemoryListener memoryListener;
    public static NetworkListener networkListener;
    public static NextTimeFlushListener nextTimeFlushListener;

    /**
     * public interface for tracking Event without custom properties
     */
    public static void trackEvent(@NonNull String event) {
        appEventLogger.track(event, null);
    }

    /**
     * public interface for tracking Event with custom properties
     */
    public static void trackEvent(@NonNull String event, @Nullable TTProperty props) {
        appEventLogger.track(event, props);
    }

    /**
     * process purchases from PurchasesUpdatedListener
     */
    public static void trackPurchase(@Nullable Object purchases, @Nullable Object skuDetailsList) {
        appEventLogger.trackPurchase(Collections.singletonList(purchases), Collections.singletonList(skuDetailsList));
    }

    /**
     * FORCE_FLUSH
     */
    public static void flush() {
        appEventLogger.forceFlush();
    }

    public static void clearAll() {
        appEventLogger.clearAll();
    }

    /**
     * applicationContext getter
     */
    public static Application getApplicationContext() {
        if (ttSdk == null)
            throw new RuntimeException("TiktokBusinessSdk instance is not initialized");
        return applicationContext;
    }

    /**
     * appKey getter
     */
    public static String getAccessToken() {
        return accessToken;
    }

    /**
     * sdkInit getter
     */
    public static boolean getNetworkSwitch() {
        return networkSwitch.get();
    }

    public static boolean isGaidCollectionEnabled() {
        return gaidCollectionEnabled;
    }

    /**
     * logLevel getter
     */
    public static LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * returns api_id
     */
    public static String getAppId() {
        return appId;
    }

    /**
     * To get config and permissions from the app
     */
    public static class TTConfig {
        /* application context */
        private final Application application;
        /* api_id for api calls */
        private String appId;
        /* Access-Token for api calls */
        private String accessToken;
        /* to enable logs */
        private boolean debug = false;
        /* to enable auto event tracking */
        private boolean autoEvent = true;
        /* confirmation to read gaid */
        private boolean advertiserIDCollectionEnable = true;
        /* auto init flag check in manifest */
        private boolean autoStart = true;

        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();

            /* try fetch app key from AndroidManifest file first */

            ApplicationInfo appInfo = null;
            try {
                appInfo = application.getPackageManager().getApplicationInfo(
                        application.getPackageName(), PackageManager.GET_META_DATA);
            } catch (Exception e) {
                TTCrashHandler.handleCrash(TAG, e);
            }

            if (appInfo == null) return;

            try {
                Object token = appInfo.metaData.get("com.tiktok.sdk.AccessToken");
                if (token != null) {
                    accessToken = token.toString();
                }
            } catch (Exception ignored) {
            }

            try {
                Object aid = appInfo.metaData.get("com.tiktok.sdk.AppId");
                if (aid != null) {
                    appId = aid.toString();
                }
            } catch (Exception ignored) {
            }

            try {
                Object autoFlag = appInfo.metaData.get("com.tiktok.sdk.turnOffAutoTracking");
                if (autoFlag != null && autoFlag.toString().equals("true")) {
                    autoStart = false;
                }
            } catch (Exception ignored) {
            }

            try {
                Object autoEventFlag = appInfo.metaData.get("com.tiktok.sdk.turnOffAutoEvents");
                if (autoEventFlag != null && autoEventFlag.toString().equals("true")) {
                    autoEvent = false;
                }
            } catch (Exception ignored) {
            }
        }

        /**
         * Enables debug logs
         */
        public TTConfig enableDebug() {
            this.debug = true;
            return this;
        }

        /**
         * set app id
         */
        public TTConfig setAppId(String apiId) {
            this.appId = apiId;
            return this;
        }

        /**
         * to set the access token if not in manifest file
         */
        public TTConfig setAccessToken(String key) {
            accessToken = key;
            return this;
        }

        /**
         * to disable auto event tracking & lifecycle listeners
         */
        public TTConfig turnOffAutoTracking() {
            this.autoStart = false;
            return this;
        }

        /**
         * to disable auto event tracking & lifecycle listeners
         */
        public TTConfig turnOffAutoEvents() {
            this.autoEvent = false;
            return this;
        }

        /**
         * to disable gaid in tracking
         */
        public TTConfig turnOffAdvertiserIDCollection() {
            this.advertiserIDCollectionEnable = false;
            return this;
        }
    }

    /**
     * Controls the level of logging.
     */
    public enum LogLevel {
        /* No logging. */
        NONE,
        /* Log exceptions only. */
        INFO,
        WARN,
        /* Log exceptions and print debug output. */
        DEBUG,
        /* Same as DEBUG, and log transformations in bundled integrations. */
        VERBOSE;

        public boolean log() {
            return this != NONE;
        }
    }

}
