package com.tiktok;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tiktok.util.TTConst.TTSDK_APP_BUILD;
import static com.tiktok.util.TTConst.TTSDK_APP_VERSION;

class TTActivityLifecycleCallbacks
        implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    static final String TAG = TTActivityLifecycleCallbacks.class.getCanonicalName();

    private TiktokBusinessSdk ttSdk;

    private AtomicBoolean trackedAppLifecycleEvents;
    private AtomicInteger numberOfActivities;
    private AtomicBoolean firstLaunch;

    public TTActivityLifecycleCallbacks(TiktokBusinessSdk ttSdk) {
        this.ttSdk = ttSdk;
        this.trackedAppLifecycleEvents = new AtomicBoolean(false);
        this.numberOfActivities = new AtomicInteger(1);
        this.firstLaunch = new AtomicBoolean(false);
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        if (shouldTrackDeepLinks()) {
            trackDeepLink(activity);
        }
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

            // TODO: 15/09/20 track Attribution
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App in foreground
        if (shouldTrackAppLifecycleEvents()) {
            TTProperty properties = new TTProperty();
            if (firstLaunch.get()) {
                properties
                        .put("version", ttSdk.getVersionName())
                        .put("build", ttSdk.getVersionCode());
            }
            properties.put("from_background", !firstLaunch.getAndSet(false));
            ttSdk.track("LaunchApp", properties);
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
        return ttSdk.lifecycleTrackEnable;
    }
    
    private boolean shouldTrackDeepLinks() {
        // TODO: 15/09/20 track deeplink expose interface in ttconfig
        return true;
    }

    private void trackApplicationLifecycleEvents() {
        String currentVersion = ttSdk.getVersionName();
        String currentBuild = String.valueOf(ttSdk.getVersionCode());

        // get the previous recorded version.
        String previousVersion = ttSdk.store.get(TTSDK_APP_VERSION);
        String previousBuild = ttSdk.store.get(TTSDK_APP_BUILD);

        // check and track InstallApp or UpdateApp
        if (previousBuild == null) {
            ttSdk.track("InstallApp",
                    new TTProperty()
                            .put("version", currentVersion)
                            .put("build", currentBuild));
        } else if (!currentBuild.equals(previousBuild)) {
            ttSdk.track(
                    "UpdateApp",
                    new TTProperty()
                            .put("version", currentVersion)
                            .put("build", currentBuild)
                            .put("previous_version", previousVersion)
                            .put("previous_build", previousBuild));
        }

        // update store with existing version
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(TTSDK_APP_VERSION, currentVersion);
        hm.put(TTSDK_APP_BUILD, currentBuild);
        ttSdk.store.set(hm);
    }

    private void trackDeepLink(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }

        TTProperty properties = new TTProperty();
        Uri uri = intent.getData();
        for (String parameter : uri.getQueryParameterNames()) {
            String value = uri.getQueryParameter(parameter);
            if (value != null && !value.trim().isEmpty()) {
                properties.put(parameter, value);
            }
        }

        properties.put("url", uri.toString());
        ttSdk.track("DeepLinkOpened", properties);
    }

}

