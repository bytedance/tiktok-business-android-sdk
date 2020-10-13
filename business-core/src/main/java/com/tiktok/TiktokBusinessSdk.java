package com.tiktok;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.appevents.TTAppEventLogger;
import com.tiktok.appevents.TTProperty;
import com.tiktok.util.TTLogger;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class TiktokBusinessSdk {
    static final String TAG = TiktokBusinessSdk.class.getName();

    /** Singleton instance for {@link TiktokBusinessSdk} */
    static volatile TiktokBusinessSdk ttSdk = null;
    /** {@link TTAppEventLogger} package singleton */
    static TTAppEventLogger appEventLogger;

    /** app {@link Context} */
    private static Application applicationContext;
    /** app_id */
    private static String appId;
    /** access token */
    private static String accessToken;
    /** {@link LogLevel} of initialized sdk */
    private static LogLevel logLevel;
    /** optOutAutoStart flag */
    private static AtomicBoolean sdkInit;

    /** logger util */
    TTLogger logger;

    private TiktokBusinessSdk(TTConfig ttConfig) {
        /* no app id exception */
        if (ttConfig.appId == null) throw new IllegalArgumentException("app id not found");
        appId = ttConfig.appId;
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
        sdkInit = new AtomicBoolean(ttConfig.autoStart);
    }

    /** initializeSdk */
    public static synchronized void initializeSdk(TTConfig ttConfig) {
        if (ttSdk != null) throw new RuntimeException("TiktokBusinessSdk instance already exists");
        ttSdk = new TiktokBusinessSdk(ttConfig);
        appEventLogger = new TTAppEventLogger(ttSdk,
                ttConfig.lifecycleTrackEnable,
                ttConfig.advertiserIDCollectionEnable);
    }

    /** startTracking if optOutAutoStart enabled */
    public static void startTracking() {
        sdkInit.set(true);
        appEventLogger.flush();
    }

    /** public interface for tracking Event without custom properties */
    public static void trackEvent(@NonNull String event) {
        appEventLogger.track(event, null);
    }

    /** public interface for tracking Event with custom properties */
    public static void trackEvent(@NonNull String event, @Nullable TTProperty props) {
        appEventLogger.track(event, props);
    }

    /** cache sku details */
    public static void cacheSkuDetails(@Nullable Object skuDetailsList) {
        assert skuDetailsList != null;
        appEventLogger.cacheSkuDetails(Collections.singletonList(skuDetailsList));
    }

    /** process purchases from PurchasesUpdatedListener */
    public static void onPurchasesUpdated(@Nullable Object purchases) {
        assert purchases != null;
        appEventLogger.trackPurchase(Collections.singletonList(purchases));
    }

    /** FORCE_FLUSH */
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

    /** sdkInit getter */
    public static boolean isSdkFullyInitialized() {
        return sdkInit.get();
    }

    /** logLevel getter */
    public static LogLevel getLogLevel() {
        return logLevel;
    }

    /** returns api_id */
    public static String getAppId() {
        return appId;
    }

    /** To get config and permissions from the app */
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
        private boolean lifecycleTrackEnable = true;
        /* confirmation to read gaid */
        private boolean advertiserIDCollectionEnable = true;
        /* auto init flag check in manifest */
        private boolean autoStart = true;

        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();

            /* try fetch app key from AndroidManifest file first */
            try {
                ApplicationInfo appInfo = application.getPackageManager().getApplicationInfo(
                        application.getPackageName(), PackageManager.GET_META_DATA);
                Object token = appInfo.metaData.get("com.tiktok.sdk.AccessToken");
                accessToken = token.toString();
                Object autoFlag = appInfo.metaData.get("com.tiktok.sdk.optOutAutoStart");
                if (autoFlag.toString().equals("true")) {
                    autoStart = false;
                }
                Object aid = appInfo.metaData.get("com.tiktok.sdk.AppId");
                appId = aid.toString();
            } catch (Exception ignored) {}
        }

        /** Enables debug logs */
        public TTConfig enableDebug() {
            this.debug = true;
            return this;
        }

        /** set app id */
        public TTConfig setAppId(String apiId) {
            this.appId = apiId;
            return this;
        }

        /** to set the access token if not in manifest file */
        public TTConfig setAccessToken(String key) {
            accessToken = key;
            return this;
        }

        /** to disable auto event tracking & lifecycle listeners */
        public TTConfig optOutAutoEventTracking() {
            this.lifecycleTrackEnable = false;
            return this;
        }

        /** to disable gaid in tracking */
        public TTConfig optOutAdvertiserIDCollection() {
            this.advertiserIDCollectionEnable = false;
            return this;
        }
    }

    /** Controls the level of logging. */
    public enum LogLevel {
        /* No logging. */
        NONE,
        /* Log exceptions only. */
        INFO,
        /* Log exceptions and print debug output. */
        DEBUG,
        /* Same as DEBUG, and log transformations in bundled integrations. */
        VERBOSE;
        public boolean log() {
            return this != NONE;
        }
    }

}
