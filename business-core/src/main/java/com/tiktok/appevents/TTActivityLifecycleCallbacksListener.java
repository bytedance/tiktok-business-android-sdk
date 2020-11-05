/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.TTLogger;

class TTActivityLifecycleCallbacksListener extends TTLifeCycleCallbacksAdapter {

    private static final String TAG = TTActivityLifecycleCallbacksListener.class.getCanonicalName();
    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private final TTAppEventLogger appEventLogger;
    private boolean isPaused = false;

    public TTActivityLifecycleCallbacksListener(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (isPaused) {
            appEventLogger.restartScheduler();
            appEventLogger.autoEventsManager.track2DayRetentionEvent();
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

