package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;

public class TTCrashHandler {
    private static final String TAG = TTCrashHandler.class.getCanonicalName();
    private static TTLogger ttLogger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private static volatile TTCrashHandler instance = new TTCrashHandler();

    public static void handleCrash(String originTag, Throwable t) {
        // will connect to online error reporting system in the future
        // suppress errors locally for the time being
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
