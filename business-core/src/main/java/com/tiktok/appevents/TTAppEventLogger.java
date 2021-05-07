/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TTAppEventLogger {
    static final String SKIP_FLUSHING_BECAUSE_NULL_ACCESS_TOKEN = "Skip flushing because access token is null";
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF = "Skip flushing because global switch is turned off";
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED = "Skip flushing because global config is not fetched";
    static final String TAG = TTAppEventLogger.class.getName();

    // every TIME_BUFFER seconds, a flush task will be pushed to the execution queue
    private static final int TIME_BUFFER = 15;
    // once THRESHOLD events got accumulated in the memory, a flush task will be pushed to the execution queue
    static final int THRESHOLD = 100;
    public static final String NETWORK_IS_TURNED_OFF = "SDK can't send tracking events to server, it will be cached locally, and will be sent in batches only after startTracking";

    static int totalDumped = 0;

    // whether to trigger automatic events in the lifeCycle callbacks provided by Android
    final boolean lifecycleTrackEnable;
    // custom auto event disable, events will be disabled when disabledEvents.contains(event)
    final List<TTConst.AutoEvents> disabledEvents;
    /**
     * Logger util
     */
    TTLogger logger;
    /**
     * Lifecycle
     */
    Lifecycle lifecycle;

    // for internal debug purpose
    int flushId = 0;

    // similar to what javascript has, so that all the internal tasks are executed in a waterfall fashion, avoiding race conditions
    static ScheduledExecutorService eventLoop = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    ScheduledFuture<?> future = null;

    // used by internal monitor
    static ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    ScheduledFuture<?> timeFuture = null;
    private final Runnable batchFlush = () -> flush(FlushReason.TIMER);

    final TTAutoEventsManager autoEventsManager;

    public static List<TTAppEvent> getSuccessfulEvents() {
        return TTRequest.getSuccessfullySentRequests();
    }

    public TTAppEventLogger(boolean lifecycleTrackEnable, List<TTConst.AutoEvents> disabledEvents) {
        logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.disabledEvents = disabledEvents;

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacksListener activityLifecycleCallbacks = new TTActivityLifecycleCallbacksListener(this);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        /** advertiser id fetch */
        autoEventsManager = new TTAutoEventsManager(this);

        SystemInfoUtil.initUserAgent();
        addToQ(TTAppEventsQueue::clearAll);
        if (TikTokBusinessSdk.getAccessToken() != null) {
            fetchGlobalConfig(0);
        } else {
            logger.info("Global config fetch is skipped because access token is empty");
        }
    }


    /**
     * persist events to the disk
     */
    void persistEvents() {
        addToQ(() -> TTAppEventStorage.persist(null));
    }

    public void trackPurchase(List<TTPurchaseInfo> purchaseInfos) {
        if (!TikTokBusinessSdk.isSystemActivated()) {
            logger.info("Global switch is off, ignore track purchase");
            return;
        }
        addToQ(() -> {
            if (purchaseInfos.isEmpty()) {
                return;
            }

            for (TTPurchaseInfo purchaseInfo : purchaseInfos) {
                JSONObject property = TTInAppPurchaseManager.getPurchaseProps(purchaseInfo);
                if (property != null) {
                    track("Purchase", property);
                }
            }
        });
    }

    int counter = 15;

    void startScheduler() {
        doStartScheduler(TIME_BUFFER, false);
    }

    void restartScheduler() {
        doStartScheduler(TIME_BUFFER, true);
    }

    /**
     * Try to flush to network every {@link TTAppEventLogger#TIME_BUFFER} seconds
     * Like setTimeInterval in js
     */
    private void doStartScheduler(int interval, boolean immediate) {
        if (future == null) {
            future = eventLoop.scheduleAtFixedRate(batchFlush, immediate ? 0 : interval, interval, TimeUnit.SECONDS);
        }
        if (timeFuture == null && TikTokBusinessSdk.nextTimeFlushListener != null) {
            counter = interval;
            timeFuture = timerService.scheduleAtFixedRate(() -> {
                TikTokBusinessSdk.nextTimeFlushListener.timeLeft(counter);
                if (counter == 0) {
                    counter = interval;
                }
                counter--;
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the recurrent task when the user interface is no longer interactive
     */
    void stopScheduler() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (timeFuture != null) {
            timeFuture.cancel(false);
            timeFuture = null;
        }
    }

    public void identify(String externalId,
                         @Nullable String externalUserName,
                         @Nullable String phoneNumber,
                         @Nullable String email) {
        TTUserInfo sharedInstance = TTUserInfo.sharedInstance;
        if (sharedInstance.isIdentified()) {
            logger.warn("SDK is already identified, if you want to switch to another" +
                    "user account, plz call TiktokBusinessSDK.logout() first and then identify");
            return;
        }
        sharedInstance.setIdentified();
        sharedInstance.setExternalId(externalId);
        sharedInstance.setExternalUserName(externalUserName);
        sharedInstance.setPhoneNumber(phoneNumber);
        sharedInstance.setEmail(email);
        trackEvent(TTAppEvent.TTAppEventType.identify, null, null);
        flushWithReason(TTAppEventLogger.FlushReason.IDENTIFY);
    }

    public void logout() {
        TTUserInfo.reset(TikTokBusinessSdk.getApplicationContext(), true);
        flushWithReason(TTAppEventLogger.FlushReason.LOGOUT);
    }

    /**
     * interface exposed to {@link TikTokBusinessSdk}
     *
     * @param event
     * @param props
     */
    public void track(String event, @Nullable JSONObject props) {
        trackEvent(TTAppEvent.TTAppEventType.track, event, props);
    }

    private void trackEvent(TTAppEvent.TTAppEventType type, String event, @Nullable JSONObject props) {
        if (!TikTokBusinessSdk.isSystemActivated()) {
            return;
        }

        JSONObject finalProps = props != null ? props : new JSONObject();
        Runnable task = () -> {
            try {
                logger.debug("track " + event + " : " + finalProps.toString(4));
            } catch (JSONException e) {
            }

            TTAppEventsQueue.addEvent(new TTAppEvent(type, event, finalProps.toString()));

            if (TTAppEventsQueue.size() > THRESHOLD) {
                flush(FlushReason.THRESHOLD);
            }
        };
        addToQ(task);
    }


    public void forceFlush() {
        flushWithReason(FlushReason.FORCE_FLUSH);
    }

    public void flushWithReason(FlushReason reason) {
        logger.debug(reason.name() + " triggered flush");
        addToQ(() -> flush(reason));
    }

    // only when this method is called will the whole sdk be activated
    private void activateSdk() {
        autoEventsManager.trackOnAppOpenEvents();
        startScheduler();
        flush(FlushReason.START_UP);
    }

    void flush(FlushReason reason) {
        TTUtil.checkThread(TAG);

        // if global config is not fetched, we can track events and put in into memory
        // but they should not be sent to the network
        if (!TikTokBusinessSdk.isGlobalConfigFetched()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED);
            return;
        }
        // global switch is turned off, dump all events
        if (!TikTokBusinessSdk.isSystemActivated()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF);
            return;
        }
        String accessToken = TikTokBusinessSdk.getAccessToken();
        if (accessToken == null) {
            logger.warn(SKIP_FLUSHING_BECAUSE_NULL_ACCESS_TOKEN);
            return;
        }

        try {
            if (TikTokBusinessSdk.getNetworkSwitch()) {
                logger.debug("Start flush, version %d reason is %s", flushId, reason.name());

                TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

                appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

                List<TTAppEvent> failedEvents = TTRequest
                        .reportAppEvent(TTRequestBuilder.getBasePayload(), appEventPersist.getAppEvents());

                if (!failedEvents.isEmpty()) { // flush failed, persist events
                    logger.debug("Failed to send %d events, will save to disk", failedEvents.size());
                    TTAppEventStorage.persist(failedEvents);
                }
                logger.debug("END flush, version %d reason is %s", flushId, reason.name());

                flushId++;
            } else {
                logger.info(NETWORK_IS_TURNED_OFF);
                TTAppEventStorage.persist(null);
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
    }

    public void destroy() {
        TTAppEventsQueue.clearAll();
        stopScheduler();
    }

    /**
     * flush reasons
     */
    public enum FlushReason {
        THRESHOLD, // when reaching the threshold of the event queue
        TIMER, // triggered every 15 seconds
        START_UP, // when app is started, flush all the accumulated events
        FORCE_FLUSH, // when developer calls flush from app
        IDENTIFY,// when calling identify
        LOGOUT,//when logging out
    }

    private void addToQ(Runnable task) {
        // http://www.javabyexamples.com/handling-exceptions-from-executorservice-tasks
        try {
            eventLoop.execute(task);
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
    }


    // Do not remove, for the ease of local test
    private void addToLater(Runnable task, int seconds) {
        // http://www.javabyexamples.com/handling-exceptions-from-executorservice-tasks
        try {
            eventLoop.schedule(task, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
    }

    public void clearAll() {
        addToQ(this::clearAllImmediately);
    }

    private void clearAllImmediately() {
        TTAppEventsQueue.clearAll();
        TTAppEventStorage.clearAll();
    }

    /**
     * set remote switch and api available version
     * if the remote config is not fetched, the events can only be saved in memory
     * if the config is fetched and config.globalSwitch is true, events can be saved in memory or on the disk.
     * if the config is fetched and config.globalSwitch is false, the events can neither be saved in memory nor on the disk
     * any events in the memory will be gone when the app is closed.
     */
    public void fetchGlobalConfig(int delaySeconds) {
        addToLater(() -> {
            try {
                logger.info("Fetching global config....");

                Map<String, Object> options = new HashMap<>();
                options.put("disable" + TTConst.AutoEvents.InstallApp.name,
                        !this.autoEventsManager.shouldTrackAppLifecycleEvents(TTConst.AutoEvents.InstallApp));
                options.put("disable" + TTConst.AutoEvents.LaunchAPP.name,
                        !this.autoEventsManager.shouldTrackAppLifecycleEvents(TTConst.AutoEvents.LaunchAPP));
                options.put("disable" + TTConst.AutoEvents.SecondDayRetention.name,
                        !this.autoEventsManager.shouldTrackAppLifecycleEvents(TTConst.AutoEvents.SecondDayRetention));

                JSONObject requestResult = TTRequest.getBusinessSDKConfig(options);

                if (requestResult == null) {
                    logger.info("Opt out of initGlobalConfig because global config is null, either api returns error or access token is not correct");
                    return;
                }

                JSONObject businessSdkConfig = (JSONObject) requestResult.get("business_sdk_config");
                Boolean enableSDK = (Boolean) businessSdkConfig.get("enable_sdk");
                String availableVersion = (String) businessSdkConfig.get("available_version");
                String trackEventDomain = (String) businessSdkConfig.get("domain");

                TikTokBusinessSdk.setSdkGlobalSwitch(enableSDK);
                logger.debug("enable_sdk=" + enableSDK);
                // if sdk is shutdown, stop all the timers
                if (!enableSDK) {
                    logger.info("Clear all events and stop timers because global switch is not turned on");
                    clearAllImmediately();
                }
                TikTokBusinessSdk.setApiAvailableVersion(availableVersion);
                TikTokBusinessSdk.setApiTrackDomain(trackEventDomain);
                logger.debug("available_version=" + availableVersion);
                TikTokBusinessSdk.setGlobalConfigFetched();
            } catch (JSONException e) {
                e.printStackTrace();
                logger.warn("Errors happened during initGlobalConfig because the structure of api result is not correct");
            } catch (Exception e){
                logger.warn("Errors occurred during initGlobalConfig because of " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (TikTokBusinessSdk.isSystemActivated() && !TikTokBusinessSdk.isActivatedLogicRun) {
                    TikTokBusinessSdk.isActivatedLogicRun = true;
                    activateSdk();
                }
            }
        }, delaySeconds);
    }
}