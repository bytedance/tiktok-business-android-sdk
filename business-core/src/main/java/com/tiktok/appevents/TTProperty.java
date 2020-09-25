package com.tiktok.appevents;

import org.json.JSONException;
import org.json.JSONObject;

/** JSONObject wrapper for custom event properties */
public class TTProperty {
    private final JSONObject properties;

    public TTProperty() {
        properties = new JSONObject();
    }

    public TTProperty put(String key, Object value) {
        try {
            properties.put(key, value);
        } catch (JSONException ignored) {}
        return this;
    }

    public JSONObject get() {
        return properties;
    }
}
