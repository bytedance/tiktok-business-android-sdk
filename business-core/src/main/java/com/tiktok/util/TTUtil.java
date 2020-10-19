package com.tiktok.util;

import android.os.Looper;

import com.tiktok.appevents.TTCrashHandler;

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
}
