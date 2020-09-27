package com.tiktok.appevents;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.tiktok.util.TTConst;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tiktok.util.TTConst.TTSDK_APP_BUILD;
import static com.tiktok.util.TTConst.TTSDK_APP_VERSION;

class TTActivityLifecycleCallbacks
        implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    static final String TAG = TTActivityLifecycleCallbacks.class.getCanonicalName();

    private final TTAppEventLogger appEventLogger;

    /** This bool checks initial events are triggered */
    private final AtomicBoolean trackedAppLifecycleEvents;
    private final AtomicInteger numberOfActivities;
    private final AtomicBoolean firstLaunch;

    public TTActivityLifecycleCallbacks(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
        this.trackedAppLifecycleEvents = new AtomicBoolean(false);
        this.numberOfActivities = new AtomicInteger(1);
        this.firstLaunch = new AtomicBoolean(false);
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityPreStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPreResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPrePaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPreStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        TTAppEventStorage.persist(null);
    }

    @Override
    public void onActivityPostStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPreSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityPostSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityPreDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPostDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        // App created
        if (!trackedAppLifecycleEvents.getAndSet(true)
                && shouldTrackAppLifecycleEvents()) {

            numberOfActivities.set(0);
            firstLaunch.set(true);
            trackApplicationLifecycleEvents();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App in foreground
        if (shouldTrackAppLifecycleEvents()) {
            if (firstLaunch.getAndSet(false)) {
                appEventLogger.track("LaunchApp", null);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new Date());
            String dateFromStore = appEventLogger.store.get(TTConst.TTSDK_APP_LAST_LAUNCH);
            try {
                Calendar lastOpen = Calendar.getInstance();
                lastOpen.setTime(dateFormat.parse(dateFromStore));
                lastOpen.add(Calendar.DATE, 1);
                if (today.equals(dateFormat.format(lastOpen.getTime()))) {
                    appEventLogger.track("2Dretention", null);
                }
            } catch (Exception ignored) {}
            appEventLogger.store.set(TTConst.TTSDK_APP_LAST_LAUNCH, today);
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App in background
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }

    private boolean shouldTrackAppLifecycleEvents() {
        return appEventLogger.lifecycleTrackEnable;
    }

    private void trackApplicationLifecycleEvents() {
        /** gets current app version & build */
        String currentVersion = appEventLogger.getVersionName();
        String currentBuild = String.valueOf(appEventLogger.getVersionCode());

        /** get the previous recorded version. */
        String previousVersion = appEventLogger.store.get(TTSDK_APP_VERSION);
        String previousBuild = appEventLogger.store.get(TTSDK_APP_BUILD);

        /** check and track InstallApp. */
        if (previousBuild == null) {
            appEventLogger.track("InstallApp", null);
        } else if (!currentBuild.equals(previousBuild)) {
            // app updated
        }

        /** update store with existing version. */
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(TTSDK_APP_VERSION, currentVersion);
        hm.put(TTSDK_APP_BUILD, currentBuild);
        appEventLogger.store.set(hm);
    }

}

