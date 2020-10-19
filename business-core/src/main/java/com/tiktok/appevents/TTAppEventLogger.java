package com.tiktok.appevents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
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
    static final String TAG = TTAppEventLogger.class.getName();

    private static final int TIME_BUFFER = 15;
    static final int THRESHOLD = 100;

    final boolean lifecycleTrackEnable;

    /**
     * Logger util
     */
    TTLogger logger;
    /**
     * Lifecycle
     */
    Lifecycle lifecycle;

    int flushId = 0;

    static ScheduledExecutorService eventLoop = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    static ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor(new TTThreadFactory());
    ScheduledFuture<?> future = null;
    ScheduledFuture<?> timeFuture = null;
    private final Runnable batchFlush = () -> flush(FlushReason.TIMER);

    private TTAutoEventsManager autoEventsManager;

    public static List<TTAppEvent> getSuccessfulEvents() {
        return TTRequest.getSuccessfullySentRequests();
    }

    public TTAppEventLogger(TiktokBusinessSdk ttSdk,
                            boolean lifecycleTrackEnable) {
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacksListener activityLifecycleCallbacks = new TTActivityLifecycleCallbacksListener(this);
        TiktokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        /** advertiser id fetch */
        autoEventsManager = new TTAutoEventsManager(this);

        remoteSdkConfigProcess();

        activateApp();

        /**
         * the main thread sleeps for 500 ms, let the remoteSdkConfigProcess method execute first
         */
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void persistEvents() {
        addToQ(() -> TTAppEventStorage.persist(null));
    }

    /**
     * track purchase after PurchasesUpdatedListener
     */
    public void trackPurchase(List<Object> purchases, List<Object> skuDetails) {
        if (!isSystemActivated()) {
            return;
        }
        addToQ(() -> {
            JSONObject allSkuMap = null;
            if (!skuDetails.isEmpty()) {
                allSkuMap = TTInAppPurchaseManager.getSkuDetailsMap(skuDetails);
            }
            if (!purchases.isEmpty()) {
                for (Object purchase : purchases) {
                    track("Purchase", TTInAppPurchaseManager.getPurchaseProps(purchase, allSkuMap));
                }
            }
        });
    }

    int counter = 15;

    /**
     * Try to flush to network every {@link TTAppEventLogger#TIME_BUFFER} seconds
     * Like setTimeInterval in js
     */
    void startScheduler() {
        doStartScheduler(TIME_BUFFER);
    }

    // for the sake of simplicity of unit tests
    private void doStartScheduler(int interval) {
        if (future == null) {
            future = eventLoop.scheduleAtFixedRate(batchFlush, interval, interval, TimeUnit.SECONDS);
        }
        if (timeFuture == null && TiktokBusinessSdk.nextTimeFlushListener != null) {
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
    public void track(@NonNull String event, @Nullable TTProperty props) {
        if (!isSystemActivated()) {
            return;
        }
        if (props == null) props = new TTProperty();
        TTProperty finalProps = props;
        Runnable task = () -> {
            logger.debug(event + " : " + finalProps.get().toString());

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
    private void activateApp() {
        SystemInfoUtil.initUserAgent();
        addToQ(() -> {
            autoEventsManager.trackOnAppOpenEvents();
            startScheduler();
            flush(FlushReason.START_UP);
        });
    }

    /**
     * if globalSwitch request is sent to network, but the network returns error, activate the app regardless
     * if globalSwitch request is sent to network and api returns false, then sdk will not be activated
     * if globalSwitch request is sent to network and api returns true, then check whether adInfoRun is set to true
     */
    private boolean isSystemActivated() {
        Boolean sdkGlobalSwitch = TiktokBusinessSdk.getSdkGlobalSwitch();
        if (!sdkGlobalSwitch) {
            logger.verbose("Global switch is off, ignore all operations");
        }
        return sdkGlobalSwitch;
    }


    private void flush(FlushReason reason) {

        if (!isSystemActivated()) return;

        TTUtil.checkThread(TAG);

        try {
            if (TiktokBusinessSdk.getNetworkSwitch()) {
                logger.verbose("Start flush, version %d reason is %s", flushId, reason.name());

                TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

                appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

                List<TTAppEvent> failedEvents = TTRequest.appEventReport(TTRequestBuilder.getBasePayload(
                        TiktokBusinessSdk.getApplicationContext(),
                        TiktokBusinessSdk.isGaidCollectionEnabled()
                        ), appEventPersist.getAppEvents());

                if (!failedEvents.isEmpty()) { // flush failed, persist events
                    logger.warn("Failed to send %d events, will save to disk", failedEvents.size());
                    TTAppEventStorage.persist(failedEvents);
                }
                logger.verbose("END flush, version %d reason is %s", flushId, reason.name());

                flushId++;
            } else {
                logger.verbose("SDK can't send tracking events to server, it will be cached locally, and will be sent in batches only after startTracking");
                TTAppEventStorage.persist(null);
            }
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
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

    public void clearAll() {
        addToQ(() -> {
            TTAppEventsQueue.clearAll();
            TTAppEventStorage.clearAll();
        });
    }

    /**
     * set remote switch and api available version
     */
    private void remoteSdkConfigProcess(){
        addToQ(()->{
            try {
                JSONObject requestResult = TTRequest.getBusinessSDKConfig();

                if(requestResult == null) return;

                JSONObject businessSdkConfig = (JSONObject)requestResult.get("business_sdk_config");

                if(businessSdkConfig == null) return;

                Boolean enableSDK = (Boolean)businessSdkConfig.get("enable_sdk");
                String availableVersion= (String) businessSdkConfig.get("available_version");

                if(enableSDK != null) {
                    TiktokBusinessSdk.setSdkGlobalSwitch(enableSDK);
                    logger.verbose("enable_sdk="+enableSDK);
                }

                if(availableVersion != null && !availableVersion.equals("")) {
                    TiktokBusinessSdk.setApiAvailableVersion(availableVersion);
                    logger.verbose("available_version="+availableVersion);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        });
    }

}
