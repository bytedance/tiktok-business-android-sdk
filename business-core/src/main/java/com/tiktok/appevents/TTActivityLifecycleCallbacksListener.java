package com.tiktok.appevents;

import androidx.lifecycle.LifecycleOwner;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tiktok.util.TTConst.TTSDK_APP_BUILD;
import static com.tiktok.util.TTConst.TTSDK_APP_VERSION;

class TTActivityLifecycleCallbacksListener extends TTLifeCycleCallbacksAdapter {

    private static final String TAG = TTActivityLifecycleCallbacksListener.class.getCanonicalName();
    private static final TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private final TTAppEventLogger appEventLogger;
    private boolean isPaused = false;

    public TTActivityLifecycleCallbacksListener(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        if (isPaused) {
            appEventLogger.restartScheduler();
        }
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        appEventLogger.stopScheduler();
        isPaused = true;
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        appEventLogger.persistEvents();
    }

    // TODO might never be called as per Android's doc
    @Override
    public void onDestroy(LifecycleOwner owner) {
        appEventLogger.stopScheduler();
    }

}

