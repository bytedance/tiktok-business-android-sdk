package com.tiktok.appevents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TTInAppPurchaseManager {
    static final String TAG = TTInAppPurchaseManager.class.getName();

    /**
     * p
     */
    static TTProperty getPurchaseProps(TTPurchaseInfo purchaseInfo) {
        String productId = null;
        try {
            productId = purchaseInfo.getPurchase().getString("productId");
        } catch (JSONException e) {
            // this exception should happen since we already did a filtering in the previous step
            TTCrashHandler.handleCrash(TAG, e);
            return null;
        }
        if (productId != null) {
            JSONObject skuDetail = purchaseInfo.getSkuDetails();
            return getPurchaseProperties(productId, skuDetail);
        } else {
            return null;
        }
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
