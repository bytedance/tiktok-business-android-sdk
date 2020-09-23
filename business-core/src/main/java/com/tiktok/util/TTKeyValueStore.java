package com.tiktok.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import static com.tiktok.util.TTConst.TTSDK_KEY_VALUE_STORE;

public class TTKeyValueStore {
    private final SharedPreferences preferences;

    public TTKeyValueStore(Context ctx) {
        preferences = ctx.getApplicationContext().getSharedPreferences(TTSDK_KEY_VALUE_STORE, Context.MODE_PRIVATE);
    }

    public String get(String key) {
        return preferences.getString(key, null);
    }

    public void set(String key, Object value) {
        preferences.edit().putString(key, value.toString()).apply();
    }

    public void set(HashMap<String, Object> data) {
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue().toString());
        }
        editor.apply();
    }
}