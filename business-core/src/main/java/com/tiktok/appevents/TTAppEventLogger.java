package com.tiktok.appevents;

import android.content.pm.PackageInfo;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTAppEventLogger {
    static final String TAG = TTAppEventLogger.class.getName();

    private static final int TIME_BUFFER = 15;
    static final int THRESHOLD = 100;

    final boolean lifecycleTrackEnable;
    final boolean advertiserIDCollectionEnable;

    /**
     * Logger util
     */
    TTLogger logger;
    /**
     * SharedPreferences util
     */
    TTKeyValueStore store;
    /**
     * packageInfo
     */
    PackageInfo packageInfo = null;
    /**
     * Lifecycle
     */
    Lifecycle lifecycle;

    private boolean isGlobalSwitchOn = false;

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
                            boolean lifecycleTrackEnable,
                            boolean advertiserIDCollectionEnable) {
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.advertiserIDCollectionEnable = advertiserIDCollectionEnable;
        /* SharedPreferences helper */
        store = new TTKeyValueStore(TiktokBusinessSdk.getApplicationContext());

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacksListener activityLifecycleCallbacks = new TTActivityLifecycleCallbacksListener(this);
        TiktokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        /** advertiser id fetch */
        autoEventsManager = new TTAutoEventsManager(this);

        //fetch global switch here
        isGlobalSwitchOn = true;
        activateApp();
    }

    public void persistEvents() {
        addToQ(() -> TTAppEventStorage.persist(null));
    }

    /**
     * local cache sku details for future track purchase
     */
    public void cacheSkuDetails(List<Object> skuDetails) {
        Runnable task = () -> {
            JSONObject allSkus = new JSONObject();
            for (Object skuDetail : skuDetails) {
                JSONObject skuJson = extractJsonFromString(skuDetail.toString());
                try {
                    String productId = skuJson.getString("productId");
                    allSkus.put(productId, skuJson);
                } catch (JSONException ignored) {
                }
            }
            saveSkuDetails(allSkus);
        };
        addToQ(task);
    }

    /**
     * track purchase after PurchasesUpdatedListener
     */
    public void trackPurchase(List<Object> purchases) {
        if (!isSystemActivated()) {
            return;
        }
        Runnable task = () -> {
            for (Object purchase : purchases) {
                JSONObject purchaseJson = extractJsonFromString(purchase.toString());
                TTProperty props = new TTProperty();
                try {
                    String productId = purchaseJson.getString("productId");
                    /** trying to get other props from cached sku store */
                    props = getPurchaseProperties(productId);
                } catch (JSONException ignored) {
                }
                track("Purchase", props);
            }
        };
        addToQ(task);
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
            future = eventLoop.scheduleAtFixedRate(batchFlush, 0, interval, TimeUnit.SECONDS);
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
        if (!isGlobalSwitchOn) {
            logger.verbose("Global switch is off, ignore all operations");
        }
        return this.isGlobalSwitchOn;
    }


    private void flush(FlushReason reason) {

        if (!isSystemActivated()) return;
        TTUtil.checkThread(TAG);

        try {
            if (TiktokBusinessSdk.getNetworkSwitch()) {
                logger.verbose("Start flush, version %d reason is %s", flushId, reason.name());

                TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

                appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

                List<TTAppEvent> failedEvents = TTRequest.appEventReport(TiktokBusinessSdk.getApplicationContext(), appEventPersist.getAppEvents());

                if (!failedEvents.isEmpty()) {//flush failed, persist events
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

    /**
     * purchase data and sku details are passed as list of objects
     * tries to find json substring in the string and
     * safe returns JSONObject
     *
     * @param objString
     * @return
     */
    private JSONObject extractJsonFromString(String objString) {
        /**
         * JSON string not passed for new api
         * egs: [Purchase. Json: {"packageName":"com.example","acknowledged":false,"orderId":"transactionId.android.test.purchased","productId":"android.test.purchased","developerPayload":"","purchaseTime":0,"purchaseState":0,"purchaseToken":"inapp:com.example:android.test.purchased"}]
         * SkuDetails: {"skuDetailsToken":"AEuhp4Lu4HAdf3nvnusEjwhfJQemFbKGuSQ37wM_7UJcce89YnZiBA6HJVz5vFMFbMPq","productId":"android.test.purchased","type":"inapp","price":"₹72.41","price_amount_micros":72407614,"price_currency_code":"INR","title":"Sample Title","description":"Sample description for product: android.test.purchased."}
         * this function tries to find start { and end } of json string in objString
         * */
        JSONObject jsonObject = null;
        int start = objString.indexOf("{");
        int end = objString.lastIndexOf("}");
        if ((start >= 0) && (end > start) && (end + 1 <= objString.length())) {
            try {
                jsonObject = new JSONObject(objString.substring(start, end + 1));
            } catch (JSONException ignored) {
                jsonObject = new JSONObject();
            }
        }
        return jsonObject;
    }

    /**
     * overrides sku details in store
     */
    private void saveSkuDetails(JSONObject newSkus) {
        JSONObject skuJson = null;
        String cachedSkuData = store.get(TTConst.TTSDK_SKU_DETAILS);
        if (cachedSkuData != null) {
            try {
                skuJson = new JSONObject(cachedSkuData);
                for (Iterator<String> it = newSkus.keys(); it.hasNext(); ) {
                    String sku = it.next();
                    skuJson.put(sku, newSkus.get(sku));
                }
            } catch (JSONException ignored) {
            }
        }
        if (skuJson == null) {
            skuJson = newSkus;
        }
        store.set(TTConst.TTSDK_SKU_DETAILS, skuJson.toString());
    }

    /**
     * returns purchase TTProperty from sku cache
     * returns content_id -> sku always
     */
    private TTProperty getPurchaseProperties(String sku) {
        JSONObject skuDetails = getSkuDetailsFromStore(sku);
        TTProperty props = new TTProperty().put("content_id", sku);
        if (skuDetails != null) {
            props.put("content_type", safeJsonGetString(skuDetails, "type"));
            String currencyCode = safeJsonGetString(skuDetails, "price_currency_code");
            props.put("currency", currencyCode);
            props.put("description", safeJsonGetString(skuDetails, "description"));
            String price = safeJsonGetString(skuDetails, "price");
            try {
                // trying to remove the currency symbol from price
                if (!currencyCode.equals("") && !price.equals("")) {
                    Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
                    Matcher matcher = regex.matcher(price);
                    if (matcher.find()) {
                        price = matcher.group(1);
                    }
                }
            } catch (Exception ignored) {
            }
            props.put("value", price);
        }
        return props;
    }

    /**
     * safe get key from jsonobject
     *
     * @param jsonObject
     * @param key
     * @return
     */
    private String safeJsonGetString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * get sku data from TTSDK_SKU_DETAILS cache
     *
     * @param sku
     * @return
     */
    private JSONObject getSkuDetailsFromStore(String sku) {
        JSONObject skuJson = null;
        if (sku == null) {
            return null;
        }
        String cachedSkuData = store.get(TTConst.TTSDK_SKU_DETAILS);
        if (cachedSkuData != null) {
            try {
                JSONObject allSkuJson = new JSONObject(cachedSkuData);
                skuJson = allSkuJson.getJSONObject(sku);
            } catch (JSONException ignored) {
            }
        }
        return skuJson;
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

}
