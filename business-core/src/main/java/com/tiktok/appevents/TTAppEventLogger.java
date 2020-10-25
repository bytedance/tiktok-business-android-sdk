package com.tiktok.appevents;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TTAppEventLogger {
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF = "Skip flushing because global switch is turned off";
    static final String SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED = "Skip flushing because global config is not fetched";
    static final String TAG = TTAppEventLogger.class.getName();

    // every TIME_BUFFER seconds, a flush task will be pushed to the execution queue
    private static final int TIME_BUFFER = 15;
    // once THRESHOLD events got accumulated in the memory, a flush task will be pushed to the execution queue
    static final int THRESHOLD = 100;
    public static final String NETWORK_IS_TURNED_OFF = "SDK can't send tracking events to server, it will be cached locally, and will be sent in batches only after startTracking";

    // whether to trigger automatic events in the lifeCycle callbacks provided by Android
    final boolean lifecycleTrackEnable;

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

    private TTAutoEventsManager autoEventsManager;

    public static List<TTAppEvent> getSuccessfulEvents() {
        return TTRequest.getSuccessfullySentRequests();
    }

    public TTAppEventLogger(boolean lifecycleTrackEnable) {
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacksListener activityLifecycleCallbacks = new TTActivityLifecycleCallbacksListener(this);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        /** advertiser id fetch */
        autoEventsManager = new TTAutoEventsManager(this);

        SystemInfoUtil.initUserAgent();
        addToQ(TTAppEventsQueue::clearAll);
        remoteSdkConfigProcess();
    }

    /**
     * persist events to the disk
     */
    void persistEvents() {
        addToQ(() -> TTAppEventStorage.persist(null));
    }

    public void trackPurchase(List<TTPurchaseInfo> purchaseInfos) {
        if (!TiktokBusinessSdk.isSystemActivated()) {
            return;
        }
        addToQ(() -> {
            JSONObject allSkuMap = null;

            if (purchaseInfos.isEmpty()) {
                return;
            }


            for (TTPurchaseInfo purchaseInfo : purchaseInfos) {
                TTProperty property = TTInAppPurchaseManager.getPurchaseProps(purchaseInfo);
                if (property != null) {
                    track(TTConst.AppEventName.Purchase, property);
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
        if (timeFuture == null && TiktokBusinessSdk.nextTimeFlushListener != null) {
            counter = interval;
            timeFuture = timerService.scheduleAtFixedRate(() -> {
                TiktokBusinessSdk.nextTimeFlushListener.timeLeft(counter);
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

    /**
     * interface exposed to {@link TiktokBusinessSdk}
     *
     * @param event
     * @param props
     */
    public void track(TTConst.AppEventName event, @Nullable TTProperty props) {
        if (!TiktokBusinessSdk.isSystemActivated()) {
            return;
        }

        if (props == null) props = new TTProperty();
        TTProperty finalProps = props;
        Runnable task = () -> {
            try {
                logger.debug("track " + event + " : " + finalProps.get().toString(4));
            } catch (JSONException e) {
            }

            TTAppEventsQueue.addEvent(new TTAppEvent(event, finalProps.get().toString()));

            if (TTAppEventsQueue.size() > THRESHOLD) {
                flush(FlushReason.THRESHOLD);
            }
        };
        addToQ(task);
    }


    public void forceFlush() {
        logger.verbose("FORCE_FLUSH called");
        addToQ(() -> flush(FlushReason.FORCE_FLUSH));
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
        if (!TiktokBusinessSdk.isGlobalConfigFetched()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_CONFIG_IS_NOT_FETCHED);
            return;
        }
        // global switch is turned off, dump all events
        if (!TiktokBusinessSdk.isSystemActivated()) {
            logger.info(SKIP_FLUSHING_BECAUSE_GLOBAL_SWITCH_IS_TURNED_OFF);
            return;
        }

        try {
            if (TiktokBusinessSdk.getNetworkSwitch()) {
                logger.verbose("Start flush, version %d reason is %s", flushId, reason.name());

                TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

                appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

                List<TTAppEvent> failedEvents = TTRequest.reportAppEvent(TTRequestBuilder.getBasePayload(
                        TiktokBusinessSdk.getApplicationContext()), appEventPersist.getAppEvents());

                if (!failedEvents.isEmpty()) { // flush failed, persist events
                    logger.warn("Failed to send %d events, will save to disk", failedEvents.size());
                    TTAppEventStorage.persist(failedEvents);
                }
                logger.verbose("END flush, version %d reason is %s", flushId, reason.name());

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
    enum FlushReason {
        THRESHOLD, // when reaching the threshold of the event queue
        TIMER, // triggered every 15 seconds
        START_UP, // when app is started, flush all the accumulated events
        FORCE_FLUSH, // when developer calls flush from app
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
        addToQ(() -> {
            TTAppEventsQueue.clearAll();
            TTAppEventStorage.clearAll();
        });
    }

    /**
     * set remote switch and api available version
     * if the remote config is not fetched, the events can only be saved in memory
     * if the config is fetched and config.globalSwitch is true, events can be saved in memory or on the disk.
     * if the config is fetched and config.globalSwitch is false, the events can neither be saved in memory nor on the disk
     * any events in the memory will be gone when the app is closed.
     */
    private void remoteSdkConfigProcess() {
        addToQ(() -> {
            try {
                JSONObject requestResult = TTRequest.getBusinessSDKConfig();

                if (requestResult == null) return;

                JSONObject businessSdkConfig = (JSONObject) requestResult.get("business_sdk_config");

                if (businessSdkConfig == null) return;

                Boolean enableSDK = (Boolean) businessSdkConfig.get("enable_sdk");
                String availableVersion = (String) businessSdkConfig.get("available_version");

                if (enableSDK != null) {
                    TiktokBusinessSdk.setSdkGlobalSwitch(enableSDK);
                    logger.verbose("enable_sdk=" + enableSDK);
                    // if sdk is shutdown, stop all the timers
                    if (!enableSDK) {
                        logger.info("Clear all events and stop timers because global switch is not turned on");
                        clearAll();
                    }
                }

                if (availableVersion != null && !availableVersion.equals("")) {
                    TiktokBusinessSdk.setApiAvailableVersion(availableVersion);
                    logger.verbose("available_version=" + availableVersion);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                TiktokBusinessSdk.setGlobalConfigFetched();
                if (TiktokBusinessSdk.getSdkGlobalSwitch()) {
                    activateSdk();
                }
            }
        });
    }
}