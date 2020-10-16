package com.tiktok.appevents;

import androidx.annotation.NonNull;

public class TTUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = TTUncaughtExceptionHandler.class.getCanonicalName();

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        TTCrashHandler.handleCrash(TAG, e);
    }
}
