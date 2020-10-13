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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTAppEventLogger {
    static final String TAG = TTAppEventLogger.class.getName();

    private static final int TIME_BUFFER = 15;
    private static final int THRESHOLD = 100;

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
    /**
     * advertiser id
     */
    TTIdentifierFactory.AdInfo adInfo;
    /**
     * this boolean checks the advertiser task ran status
     */
    final AtomicBoolean adInfoRun;

    int flushId = 0;

    static ScheduledExecutorService eventLoop = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> future = null;
    private final Runnable batchFlush = () -> flush(FlushReason.TIMER);

    public TTAppEventLogger(TiktokBusinessSdk ttSdk,
                            boolean lifecycleTrackEnable,
                            boolean advertiserIDCollectionEnable) {
        adInfoRun = new AtomicBoolean(false);
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        this.lifecycleTrackEnable = lifecycleTrackEnable;
        this.advertiserIDCollectionEnable = advertiserIDCollectionEnable;
        /* SharedPreferences helper */
        store = new TTKeyValueStore(TiktokBusinessSdk.getApplicationContext());
        try {
            packageInfo = TiktokBusinessSdk.getApplicationContext().getPackageManager()
                    .getPackageInfo(TiktokBusinessSdk.getApplicationContext().getPackageName(), 0);
        } catch (Exception ignored) {
        }

        lifecycle = ProcessLifecycleOwner.get().getLifecycle();

        /** ActivityLifecycleCallbacks & LifecycleObserver */
        TTActivityLifecycleCallbacks activityLifecycleCallbacks = new TTActivityLifecycleCallbacks(this);
        TiktokBusinessSdk.getApplicationContext().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        this.lifecycle.addObserver(activityLifecycleCallbacks);

        /** advertiser id fetch */
        this.runIdentifierFactory();
        startScheduler();
    }

    public void persistEvents() {
        addToQ(()-> TTAppEventStorage.persist(null));
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

    void startScheduler() {
        if (future == null) {
            future = eventLoop.scheduleAtFixedRate(batchFlush, TIME_BUFFER, TIME_BUFFER, TimeUnit.SECONDS);
        }
    }

    void stopScheduler() {
        future.cancel(false);
        future = null;
    }

    /**
     * interface exposed to {@link TiktokBusinessSdk}
     * @param event
     * @param props
     */
    public void track(@NonNull String event, @Nullable TTProperty props) {
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

    public void flush() {
        logger.verbose("FORCE_FLUSH called");
        addToQ(() -> flush(FlushReason.FORCE_FLUSH));
    }

    private void runIdentifierFactory() {
        TTIdentifierFactory.getAdvertisingId(
                TiktokBusinessSdk.getApplicationContext(), TiktokBusinessSdk.getLogLevel(),
                new TTIdentifierFactory.Listener() {
                    @Override
                    public void onIdentifierFactoryFinish(TTIdentifierFactory.AdInfo ad) {
                        adInfoRun.set(true);
                        adInfo = ad;
                        executeQueue();
                    }

                    @Override
                    public void onIdentifierFactoryFail(Exception e) {
                        adInfoRun.set(true);
                        adInfo = null;
                        logger.error(e, "unable to fetch Advertising Id");
                        executeQueue();
                    }
                });
    }

    String getVersionName() {
        if (packageInfo == null) {
            return "";
        }
        return packageInfo.versionName;
    }

    int getVersionCode() {
        if (packageInfo == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return (int) packageInfo.getLongVersionCode();
        }
        // noinspection deprecation
        return packageInfo.versionCode;
    }

    private boolean loggerInitialized() {
        return this.adInfoRun.get();
    }

    private void executeQueue() {
        if (!loggerInitialized()) return;

        addToQ(() -> flush(FlushReason.START_UP));
    }

    private void flush(FlushReason reason) {

        if (!loggerInitialized()) return;

        if (TiktokBusinessSdk.isSdkFullyInitialized()) {
            logger.verbose("Start flush, version %d reason is %s", flushId, reason.name());

            TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

            appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

            List<TTAppEvent> eventList = TTRequest.appEventReport(appEventPersist.getAppEvents());

            if (!eventList.isEmpty()) {//flush failed, persist events
                TTAppEventStorage.persist(eventList);
            }
            logger.verbose("END flush, version %d reason is %s", flushId, reason.name());

            flushId++;
        } else {
            logger.verbose("SDK can't send tracking events to server, it will be cached locally, and will be send in batches only after startTracking");
            TTAppEventStorage.persist(null);
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
     * @param objString
     * @return
     */
    private JSONObject extractJsonFromString(String objString) {
        JSONObject jsonObject = null;
        int start = objString.indexOf("{");
        int end = objString.indexOf("}");
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
            } catch (Exception ignored) {}
            props.put("value", price);
        }
        return props;
    }

    /**
     * safe get key from jsonobject
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
        try {
            eventLoop.execute(task);
        } catch (Exception ignored) {
        }
    }

}
