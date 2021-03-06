/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTLogger;

/**
 * A global crash handler which mainly does
 * 1. send error details to a remote analytic tool
 * 2. Prevent from app from crash
 */
public class TTCrashHandler {
    private static final String TAG = TTCrashHandler.class.getCanonicalName();
    private static TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

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
