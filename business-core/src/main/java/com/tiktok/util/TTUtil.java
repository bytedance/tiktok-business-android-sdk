/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

import static com.tiktok.util.TTConst.TTSDK_APP_ANONYMOUS_ID;

public class TTUtil {
    private static final String TAG = TTUtil.class.getName();
    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    /**
     * All internal operations should be pushed to the internal {@link com.tiktok.appevents.TTAppEventLogger#eventLoop}
     * and run in a non-main thread
     *
     * @param tag
     */
    public static void checkThread(String tag) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            TTCrashHandler.handleCrash(tag, new IllegalStateException("Current method should be called in a non-main thread"));
        }
    }

    /**
     * pretty print str
     *
     * @param o
     * @return
     */
    public static String ppStr(JSONObject o) {
        if (o == null) {
            return "null";
        }
        try {
            return o.toString(4);
        } catch (JSONException e) {
            return "";
        }
    }

    public static String ppStr(String str) {
        try {
            return ppStr(new JSONObject(str));
        } catch (JSONException e) {
            return "";
        }
    }

    public static String getOrGenAnoId(Context context, boolean forceGenerate) {
        TTKeyValueStore store = new TTKeyValueStore(context);
        String anoId = store.get(TTSDK_APP_ANONYMOUS_ID);
        if (anoId == null || forceGenerate) {
            // TODO make sure anoId universally unique, also limit the length.
            anoId = UUID.randomUUID().toString();
            store.set(TTSDK_APP_ANONYMOUS_ID, anoId);
            logger.info("AnonymousId reset to " + anoId);
        }
        return anoId;
    }

    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String mapToString(Map<String, Object> map, String separator) {
        if (map.isEmpty()) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            buf.append(entry.getKey() + "=" + escapeHTML(entry.getValue().toString()) + "&");
        }
        String finalStr = buf.toString();
        return finalStr.substring(0, finalStr.length() - 1);
    }
}
