package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;

public class TTCrashHandler {
    private static final String TAG = TTCrashHandler.class.getCanonicalName();
    private static TTLogger ttLogger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private static volatile TTCrashHandler instance = new TTCrashHandler();

    public static void handleCrash(String originTag, Throwable t) {
        ttLogger.error(t, "Error caused by sdk at " + originTag + "\n" + t.getMessage() + "\n"
                + getStackTrace(t)
        );
    }

    private static String getStackTrace(Throwable t) {
        StringBuffer buffer = new StringBuffer();
        for (StackTraceElement curr : t.getStackTrace()) {
            buffer.append(curr.toString() + "\n");
        }
        return buffer.toString();
    }
}
