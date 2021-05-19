/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Nullable;

import com.tiktok.appevents.TTAppEventLogger;
import com.tiktok.appevents.TTCrashHandler;
import com.tiktok.appevents.TTPurchaseInfo;
import com.tiktok.appevents.TTUserInfo;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TikTokBusinessSdk {
    static final String TAG = TikTokBusinessSdk.class.getName();

    /**
     * Singleton instance for {@link TikTokBusinessSdk}
     */
    static volatile TikTokBusinessSdk ttSdk = null;

    public static volatile boolean isActivatedLogicRun = false;

    /**
     * {@link TTAppEventLogger} package singleton
     */
    static TTAppEventLogger appEventLogger;

    private static final AtomicBoolean globalConfigFetched = new AtomicBoolean(false);

    /**
     * We provide a global switch in order that you can turn off our sdk remotely
     * This is a final rescue in case our sdk is causing constant crashes in you app.
     * If the switch is off, the events will neither be saved to the memory nor on the disk,
     * our sdk simply ignores all the track or flush requests.
     */
    private static Boolean sdkGlobalSwitch = true;
    /**
     * api available version
     */
    private static String apiAvailableVersion = "v1.1";
    /**
     * api treckEvent sent Domain
     */
    private static String apiTrackDomain = "business-api.tiktok.com";
    /**
     * {@link LogLevel} of initialized sdk
     */
    private static LogLevel logLevel = LogLevel.INFO;
    /**
     * if set to false, only save to memory and disk, no api request will be sent
     */
    private static AtomicBoolean networkSwitch;

    private static TTConfig config;
    /**
     * logger util
     */
    private static TTLogger logger;

    private TikTokBusinessSdk(TTConfig ttConfig) {
        /* sdk logger & loglevel */
        logLevel = ttConfig.logLevel;
        logger = new TTLogger(TAG, logLevel);

        /* no app id exception */
        if (ttConfig.appId == null) {
            throw new IllegalArgumentException("app id not set");
        }

        if (ttConfig.ttAppId == null) {
            logger.warn("ttAppId not set, but its usage is encouraged");
        }

        if (ttConfig.accessToken != null) {
            ttConfig.accessToken = ttConfig.accessToken.trim();
        }

        config = ttConfig;
        networkSwitch = new AtomicBoolean(ttConfig.autoStart);
    }

    public static synchronized boolean isInitialized() {
        return ttSdk != null;
    }

    /**
     * Only one TikTokBusinessSdk instance exist within a single App process
     */
    public static synchronized void initializeSdk(TTConfig ttConfig) {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> TTCrashHandler.handleCrash(TAG, e));

        if (ttSdk != null) throw new RuntimeException("TikTokBusinessSdk instance already exists");

        ttSdk = new TikTokBusinessSdk(ttConfig);


        TTUserInfo.reset(TikTokBusinessSdk.getApplicationContext(), false);
        // the appEventLogger instance will be the main interface to track events
        appEventLogger = new TTAppEventLogger(ttConfig.autoEvent, ttConfig.disabledEvents);
    }

    /**
     * Normally, the sdk will enable network after it is initialized,
     * all the events stored in the memory or on the disk will be flushed to network once some conditions are reached,
     * for example, every {@link TTAppEventLogger#TIME_BUFFER} seconds or there are more than {@link TTAppEventLogger#THRESHOLD} events
     * in the memory.
     * But if the app developer calls {@link TTConfig#disableAutoStart()} ()}, then the "flush to network" operation will be simply suppressed
     * by the sdk, then the developer has to call startTrack to bring network back.
     * <p>
     * When to use?
     * This method can be invoked after the user agrees to some terms or conditions, so that the events are not pushed to the network
     * before users' consent.
     */
    public static void startTrack() {
        if (!networkSwitch.get()) {
            networkSwitch.set(true);
            appEventLogger.forceFlush();
        }
    }

    public static void destroy() {
        ttSdk = null;
        memoryListener = null;
        diskListener = null;
        networkListener = null;
        nextTimeFlushListener = null;
        if (appEventLogger != null) {
            appEventLogger.destroy();
        }
    }

    /**
     * For internal development usage, a monitoring tool which oversees how many events are in the memory,
     * on the disk and have been flushed to network.
     *
     * @param ml
     * @param dl
     * @param nl
     * @param nfl
     */
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
        flush();
    }

    // inner status listeners, for debugging purpose
    public interface DiskStatusListener {
        void onDiskChange(int diskSize, boolean read);

        void onDumped(int dumped);
    }

    public interface NextTimeFlushListener {
        // how many seconds until next auto flush
        void timeLeft(int timeLeft);

        // how many until threshold
        // i.e. threshold is 100, current in memory is 80, then left will be 100 - 80 = 20
        void thresholdLeft(int threshold, int left);
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
     * A shortcut method for the situations where the events do not require a property body.
     * see more {@link TikTokBusinessSdk#trackEvent(String, JSONObject)}
     */
    public static void trackEvent(String event) {
        appEventLogger.track(event, null);
    }


    /**
     * <pre>
     * public interface for tracking Event with custom properties.
     * You can pass in any eventName and relevant properties as per
     * <a href="https://ads.tiktok.com/marketing_api/docs?rid=a5vmu2dmwy&id=1679472066464769">here</a>
     *
     * As everything is schemaless in this version, so we highly encourage you to construct the properties
     * conforming to what is described in the above doc.
     *
     * Purchase events could be special, since we are providing a more user-friendly way here:
     * For google play purchase event, see {@link #trackGooglePlayPurchase(TTPurchaseInfo)}
     *
     * For a more common purchase scenario, here is an example
     * {@code
     *      TTPurchaseItem item1 = new TTPurchaseItem(23.5f, 2, "a", "a");
     *      TTPurchaseItem item2 = new TTPurchaseItem(10.5f, 1, "b", "b");
     *
     *      TikTokBusinessSdk.trackEvent("Purchase", TTPurchaseItem.getPurchaseProperty("dollar", item1, item2));
     * }
     * </pre>
     *
     * @param event event name
     */
    public static void trackEvent(String event, @Nullable JSONObject props) {
        appEventLogger.track(event, props);
    }

    /**
     * Track a list of google play purchases at the same time.
     */
    public static void trackGooglePlayPurchase(List<TTPurchaseInfo> purchaseInfos) {
        appEventLogger.trackPurchase(purchaseInfos);
    }

    /**
     * Track a google play purchase, a google purchase is consisted of a Purchase Object
     * and a SkuDetails Object, which are two essential params to construct a {@link TTPurchaseInfo} object here.
     *
     * @param info
     */
    public static void trackGooglePlayPurchase(TTPurchaseInfo info) {
        trackGooglePlayPurchase(Collections.singletonList(info));
    }

    /**
     * Eagerly flush events to network.
     * Normally, all events stored in the memory or on the disk will be flushed to network once some conditions are reached,
     * for example, every {@link TTAppEventLogger#TIME_BUFFER} seconds or there are more than {@link TTAppEventLogger#THRESHOLD} events
     * in the memory.
     */
    public static void flush() {
        appEventLogger.forceFlush();
    }

    /**
     * Internal use only
     * clear all events from memory and disk
     * Calling this method is discouraged
     */
    public static void clearAll() {
        appEventLogger.clearAll();
    }

    /**
     * applicationContext getter
     */
    public static Application getApplicationContext() {
        if (ttSdk == null)
            throw new RuntimeException("TikTokBusinessSdk instance is not initialized");
        return config.application;
    }

    /**
     * appKey getter
     */
    public static String getAccessToken() {
        return config.accessToken;
    }

    public static void updateAccessToken(String accessToken) {
        if (!TikTokBusinessSdk.isInitialized()) {
            throw new IllegalStateException("This function should only be called after sdk is initialized");
        }
        if (accessToken == null) {
            throw new IllegalArgumentException("Access Token cannot be null");
        }
        TikTokBusinessSdk.config.accessToken = accessToken.trim();
        if (!isGlobalConfigFetched()) {
            logger.info("Access token updated, try to refetch global config");
            appEventLogger.fetchGlobalConfig(0);
        }
    }

    /**
     * sdkInit getter
     */
    public static boolean getNetworkSwitch() {
        return networkSwitch.get();
    }

    public static boolean isGaidCollectionEnabled() {
        return config.advertiserIDCollectionEnable;
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
        return config.appId;
    }

    /**
     * returns api_id
     */
    public static Long getTTAppId() {
        return config.ttAppId;
    }

    /**
     * if only appId is provided, the json schema would be like
     * {
     * "app_id": "1211123727",
     * "batch": [
     * {
     * "type": "track",
     * "event": "PURCHASE",
     * "timestamp": "2020-09-18T19:49:27Z",
     * "context": {
     * "app": {
     * "name": "Angry Birds Classic",
     * "namespace": "com.rovio.baba",
     * "version": "3.0.2",
     * "build": "122"
     * },
     * ....
     * }
     * ....
     * }
     * ]
     * }
     * Only setting appId is discouraged and should be deprecated in the future by making ttAppId
     * mandatory.
     * <p>
     * If both appId and ttAppId are provided, ttAppId is set at the root level while appIds
     * are moved into context.app.id.
     * {
     * "tiktok_app_id": 6899407619589406722,
     * "batch": [
     * {
     * "type": "identify",
     * "event": "INSTALL_APP",
     * "timestamp": "2020-09-17T19:49:27Z",
     * "context": {
     * "app": {
     * "id": "com.rovio.angrybirds",
     * ....
     * },
     * ...
     * }
     * ...
     * }
     * ]
     * }
     */

    public static boolean onlyAppIdProvided() {
        if(config.appId == null){
            throw new IllegalStateException("AppId should be checked before, this path should not be accessed");
        }
        return config.ttAppId == null;
    }

    /**
     * Both appId and ttAppId are provided
     * @return
     */
    public static boolean bothIdsProvided(){
        return !onlyAppIdProvided();
    }

    public static Boolean getSdkGlobalSwitch() {
        return sdkGlobalSwitch;
    }

    /**
     * if globalSwitch request is sent to network, but the network returns error, activate the app regardless,
     * because TiktokBUsinessSDK.sdkGlobalSwitch is set to true by default.
     * if globalSwitch request is sent to network and api returns false, then sdk will not be activated
     * if globalSwitch request is sent to network and api returns true, then check whether adInfoRun is set to true
     */
    public static boolean isSystemActivated() {
        Boolean sdkGlobalSwitch = TikTokBusinessSdk.getSdkGlobalSwitch();
        if (!sdkGlobalSwitch) {
            logger.info("Global switch is off, ignore all operations");
        }
        return sdkGlobalSwitch;
    }

    public static Boolean isGlobalConfigFetched() {
        return globalConfigFetched.get();
    }

    public static void setGlobalConfigFetched() {
        globalConfigFetched.set(true);
    }

    public static void setSdkGlobalSwitch(Boolean sdkGlobalSwitch) {
        TikTokBusinessSdk.sdkGlobalSwitch = sdkGlobalSwitch;
    }

    public static String getApiAvailableVersion() {
        return apiAvailableVersion;
    }

    public static void setApiAvailableVersion(String apiAvailableVersion) {
        TikTokBusinessSdk.apiAvailableVersion = apiAvailableVersion;
    }

    public static String getApiTrackDomain() {
        return apiTrackDomain;
    }

    public static void setApiTrackDomain(String apiTrackDomain) {
        TikTokBusinessSdk.apiTrackDomain = apiTrackDomain;
    }

    /**
     * Should be called whenever the user info changes <br/>
     * - when the user logins in <br/>
     * - when a new user signs up <br/>
     * - when the user updates his/her profile,logout first, then identify <br/>
     * - If the user's previous user info is remembered and the app is reopened.<br/>
     *
     * @param externalId
     * @param externalUserName
     * @param phoneNumber
     * @param email
     */
    public static synchronized void identify(String externalId,
                                             @Nullable String externalUserName,
                                             @Nullable String phoneNumber,
                                             @Nullable String email) {
        appEventLogger.identify(externalId, externalUserName, phoneNumber, email);
    }

    /**
     * Should be called when the user logs out <br/>
     * - when the app logs out <br/>
     * - when switching to another account,
     * in that case, should call a subsequent {@link TikTokBusinessSdk#identify(String, String, String, String)}
     */
    public static synchronized void logout() {
        appEventLogger.logout();
    }

    /**
     * To get config and permissions from the app
     * All config items can be set by declaring <meta-data> in AndroidManifest.xml,
     * but they can also be set explicitly by calling the relevant setters methods defined in this class
     * see more {@link TTConfig#disableAutoStart()}, {@link TTConfig#setLogLevel(LogLevel)}
     */
    public static class TTConfig {
        /* application context */
        private final Application application;
        /* api_id for api calls */
        private String appId;
        private Long ttAppId;
        /* Access-Token for api calls */
        private String accessToken;
        /* to enable logs */
        private LogLevel logLevel = LogLevel.NONE;
        /* to enable auto event tracking */
        private boolean autoEvent = true;
        /* confirmation to read gaid */
        private boolean advertiserIDCollectionEnable = true;
        /* auto init flag check in manifest */
        private boolean autoStart = true;
        /* disable custom auto events */
        private List<TTConst.AutoEvents> disabledEvents;

        /**
         * Read configs from <meta-data>
         *
         * @param context
         */
        public TTConfig(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            application = (Application) context.getApplicationContext();
            disabledEvents = new ArrayList<>();
            /* try fetch app key from AndroidManifest file first */

//            ApplicationInfo appInfo = null;
//            try {
//                appInfo = application.getPackageManager().getApplicationInfo(
//                        application.getPackageName(), PackageManager.GET_META_DATA);
//            } catch (Exception e) {
//                TTCrashHandler.handleCrash(TAG, e);
//            }
//
//            if (appInfo == null) return;
//
//            try {
//                Object token = appInfo.metaData.get("com.tiktok.sdk.AccessToken");
//                if (token != null) {
//                    accessToken = token.toString();
//                }
//            } catch (Exception ignored) {
//            }
//
//            try {
//                Object aid = appInfo.metaData.get("com.tiktok.sdk.AppId");
//                if (aid != null) {
//                    appId = aid.toString();
//                }
//            } catch (Exception ignored) {
//            }
//
//            try {
//                Object autoFlag = appInfo.metaData.get("com.tiktok.sdk.disableAutoStart");
//                if (autoFlag != null && autoFlag.toString().equals("true")) {
//                    autoStart = false;
//                }
//            } catch (Exception ignored) {
//            }
//
//            try {
//                Object autoEventFlag = appInfo.metaData.get("com.tiktok.sdk.disableAutoEvents");
//                if (autoEventFlag != null && autoEventFlag.toString().equals("true")) {
//                    autoEvent = false;
//                }
//            } catch (Exception ignored) {
//            }
        }

        /**
         * Enables debug logs
         */
        public TTConfig setLogLevel(LogLevel ll) {
            this.logLevel = ll;
            return this;
        }

        /**
         * set app id
         */
        public TTConfig setTTAppId(String ttAppId) {
            this.ttAppId = Long.valueOf(ttAppId);
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
        public TTConfig disableAutoStart() {
            this.autoStart = false;
            return this;
        }

        /**
         * to disable all auto event tracking captured by lifecycle listeners
         */
        public TTConfig disableAutoEvents() {
            this.autoEvent = false;
            return this;
        }

        /**
         * to disable auto event tracking for InstallApp event
         */
        public TTConfig disableInstallLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.InstallApp);
            return this;
        }

        /**
         * to disable auto event tracking for LaunchAPP event
         */
        public TTConfig disableLaunchLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.LaunchAPP);
            return this;
        }

        /**
         * to disable auto event tracking for 2Dretention event
         */
        public TTConfig disableRetentionLogging() {
            this.disabledEvents.add(TTConst.AutoEvents.SecondDayRetention);
            return this;
        }

        /**
         * to disable gaid in tracking
         */
        public TTConfig disableAdvertiserIDCollection() {
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
        DEBUG;

        public boolean log() {
            return this != NONE;
        }
    }

}

