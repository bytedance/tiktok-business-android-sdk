package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TTInAppPurchaseManager {
    static final String TAG = TTInAppPurchaseManager.class.getName();
    /**
     * SharedPreferences util
     */
    private static TTKeyValueStore store;
    private static TTLogger logger;

    static {
        /* SharedPreferences helper */
        store = new TTKeyValueStore(TiktokBusinessSdk.getApplicationContext());
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
    }

    /**
     * local cache sku details for future track purchase
     */
    static JSONObject getSkuDetailsMap(List<JSONObject> skuDetails) {
        JSONObject allSkus = new JSONObject();
        for (JSONObject skuDetail : skuDetails) {
            try {
                String productId = skuDetail.getString("productId");
                allSkus.put(productId, skuDetail);
            } catch (JSONException ignored) {
            }
        }
        return allSkus;
    }

    /**
     * p
     * */
    static TTProperty getPurchaseProps(JSONObject purchase, JSONObject allSkuMap) {
        TTProperty props = new TTProperty();
        String productId = null;
        try {
            productId = purchase.getString("productId");
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e);
            return null;
        }
        if (productId != null) {
            JSONObject skuDetail = null;
            if (allSkuMap != null) {
                try {
                    skuDetail = allSkuMap.getJSONObject(productId);
                } catch (JSONException e) {
                    TTCrashHandler.handleCrash(TAG, e);
                    return null;
                }
            }
            props = getPurchaseProperties(productId, skuDetail);
        }
        return props;
    }

    /**
     * purchase data and sku details are passed as list of objects
     * tries to find json substring in the string and
     * safe returns JSONObject
     *
     * @param objString
     * @return
     */
    private static JSONObject extractJsonFromString(String objString) {
        /**
         * JSON string not passed for new api
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
     * returns purchase TTProperty from sku cache
     * returns content_id -> sku always
     */
    private static TTProperty getPurchaseProperties(String sku, JSONObject skuDetails) {
        TTProperty props = new TTProperty();
        TTProperty content = new TTProperty().put("content_id", sku);
        if (skuDetails != null) {
            content.put("content_type", safeJsonGetString(skuDetails, "type"));
            String currencyCode = safeJsonGetString(skuDetails, "price_currency_code");
            props.put("currency", currencyCode);
            content.put("quantity", 1);
            content.put("description", safeJsonGetString(skuDetails, "description"));
            String price = safeJsonGetString(skuDetails, "price");
            float floatPrice = (float) 0;
            try {
                // trying to remove the currency symbol from price
                if (!currencyCode.equals("") && !price.equals("")) {
                    Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
                    Matcher matcher = regex.matcher(price);
                    if (matcher.find()) {
                        price = matcher.group(1);
                        floatPrice = Float.parseFloat(price);
                    }
                }
            } catch (Exception ignored) {
            }
            content.put("price", floatPrice);
            props.put("value", floatPrice);
        }
        props.put("contents", new JSONArray().put(content.get()));
        return props;
    }

    /**
     * safe get key from jsonobject
     *
     * @param jsonObject
     * @param key
     * @return
     */
    private static String safeJsonGetString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            return "";
        }
    }

}
