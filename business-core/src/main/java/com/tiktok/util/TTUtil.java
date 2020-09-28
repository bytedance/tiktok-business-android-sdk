package com.tiktok.util;

import android.os.Looper;

import com.tiktok.appevents.TTCrashHandler;

public class TTUtil {
    public static void checkThread(String tag) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            TTCrashHandler.handleCrash(tag, new IllegalStateException("Current method should be called in a non-main thread"));
        }
    }
}
