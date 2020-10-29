package com.tiktok.util;

import android.os.Looper;

import com.tiktok.appevents.TTCrashHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class TTUtil {
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
}
