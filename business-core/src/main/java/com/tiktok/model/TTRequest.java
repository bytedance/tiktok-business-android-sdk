package com.tiktok.model;

import android.util.Log;

import com.tiktok.util.HttpRequestUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TTRequest {
    private static String TAG = TTRequest.class.getCanonicalName();

    public static void test() {
        String url = "https://ads.tiktok.com/open_api/v1.1/advertiser/info/";
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("advertiser_ids", Arrays.asList(75960823018L));

            Map<String, String> map = new HashMap<>();
            map.put("Content-Type", "application/json");
            map.put("Connection", "Keep-Alive");
            map.put("access-token", "c2424295d21b7f32573ad5ec2a1cbb3ca92e09be");

            String result = HttpRequestUtil.doPost(url, map, jsonObject.toString());

            Log.i(TAG, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
