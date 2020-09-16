package com.tiktok;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class TTProperty {
    private JSONObject properties;

    public TTProperty() {
        properties = new JSONObject();
    }

    public TTProperty put(String key, Object value) {
        try {
            properties.put(key, value);
        } catch (JSONException ignored) {}
        return this;
    }

    JSONObject get() {
        return properties;
    }
}
