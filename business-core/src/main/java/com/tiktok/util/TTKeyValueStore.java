package com.tiktok.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.tiktok.util.TTConst;

import java.util.HashMap;
import java.util.Map;

import static com.tiktok.util.TTConst.TTSDK_KEY_VALUE_STORE;

public class TTKeyValueStore {
    private SharedPreferences preferences;
    private HashMap<String, String> store;

    public TTKeyValueStore(Context ctx) {
        preferences = ctx.getApplicationContext().getSharedPreferences(TTSDK_KEY_VALUE_STORE, Context.MODE_PRIVATE);
        store = new HashMap<>();
        this.loadAll();
    }

    public String get(String key) {
        return preferences.getString(key, null);
    }

    @SuppressLint("ApplySharedPref")
    public void set(String key, Object value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value.toString());
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    public void set(HashMap<String, Object> data) {
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue().toString());
        }
        editor.commit();
    }

    private void loadAll() {
        Map<String, ?> allKeys = preferences.getAll();
        for (Map.Entry<String, ?> entry : allKeys.entrySet()) {
            store.put(entry.getKey(), entry.getValue().toString());
        }
    }
}