package com.tiktok.appevents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSONObject wrapper for custom event properties
 */
public class TTProperty {
    private final JSONObject properties;

    public TTProperty() {
        properties = new JSONObject();
    }

    public TTProperty(JSONObject jsonObject) {
        properties = jsonObject;
    }

    public TTProperty put(String key, Object value) {
        try {
            if (value instanceof TTProperty) {
                TTProperty ttProperty = (TTProperty) value;
                properties.put(key, ttProperty.get());
            } else {
                properties.put(key, value);
            }
        } catch (JSONException ignored) {
        }
        return this;
    }

    public JSONObject get() {
        return properties;
    }

    /**
     * A more universal way to construct a purchase item
     */
    public static class PurchaseItem {
        private final float price;
        private final int quantity;
        // typically used for ecommerce and describes the retail item (e.g. women's watches)
        private final String contentType;
        // an ID for the content type (e.g. SKU)
        private final String contentId;

        public PurchaseItem(float price, int quantity, String contentType, String contentId) {
            this.price = price;
            this.quantity = quantity;
            this.contentType = contentType;
            this.contentId = contentId;
        }

        public TTProperty toTTProperty() {
            return new TTProperty()
                    .put("price", price)
                    .put("quantity", quantity)
                    .put("content_type", contentType)
                    .put("content_id", contentId);
        }
    }

    public static TTProperty getPurchaseProperty(String currency, PurchaseItem... items){
        float value = (float) 0;
        JSONArray contents = new JSONArray();
        for (PurchaseItem item : items) {
            float price = item.price;
            int quantity = item.quantity;
            contents.put(item.toTTProperty().get());
            if (price != 0 && quantity != 0) {
                value += (price * quantity);
            }
        }
        return new TTProperty()
                .put("currency", currency)
                .put("value", value)
                .put("contents", contents);
    }

}
