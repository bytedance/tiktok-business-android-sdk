package com.tiktok.appevents;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;

class TTActivityLifecycleCallbacksListener extends TTLifeCycleCallbacksAdapter {

    private static final String TAG = TTActivityLifecycleCallbacksListener.class.getCanonicalName();
    private static final TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private final TTAppEventLogger appEventLogger;
    private boolean isPaused = false;

    public TTActivityLifecycleCallbacksListener(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (isPaused) {
            appEventLogger.restartScheduler();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        appEventLogger.stopScheduler();
        isPaused = true;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        appEventLogger.persistEvents();
    }

    // TODO might never be called as per Android's doc
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        appEventLogger.stopScheduler();
    }

}

