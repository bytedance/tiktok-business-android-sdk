package com.tiktok;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tiktok.appevents.TTAppEventStorage;

public class TiktokSdk {
    private static final String TAG = TiktokSdk.class.getName();

    private static volatile TiktokSdk ttSdk = null;

    private static TTConfig config;

    private TiktokSdk(TTConfig conf) {
        config = conf;
    }

    public static TTConfig getConfig() {
        return config;
    }

    public static synchronized void initialize(TTConfig ttConfig) {
        if (ttSdk == null) {
            ttSdk = new TiktokSdk(ttConfig);
        }
    }

    public static class TTConfig {
        private static Context context;
        private static String appKey;
        private static boolean debug = false;
        private static boolean autoEventTrackEnable = true;
        private static boolean advertiserIDCollectionEnable = true;

        public Context getContext() {
            return context;
        }

        public TTConfig(Context ctx) {
            context = ctx;
            ApplicationInfo appInfo = null;
            try {
                appInfo = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("error parsing meta data");
            }
            Object key = appInfo.metaData.get("com.tiktok.business.AppKey");
            if (key instanceof String) {
                appKey = (String) key;
            } else {
                throw new IllegalArgumentException("key not found");
            }
        }

        public TTConfig enableDebug() {
            debug = true;
            return this;
        }

        public TTConfig optInAutoEventTracking() {
            autoEventTrackEnable = true;
            return this;
        }

        public TTConfig optOutAutoEventTracking() {
            autoEventTrackEnable = false;
            return this;
        }

        public TTConfig optInAdvertiserIDCollection() {
            advertiserIDCollectionEnable = false;
            return this;
        }

        public TTConfig optOutAdvertiserIDCollection() {
            advertiserIDCollectionEnable = false;
            return this;
        }
    }

    public static TiktokSdk using(Application context) {
        if (ttSdk == null) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            synchronized (TiktokSdk.class) {
                if (ttSdk == null) {
                    TTConfig ttConfig = new TTConfig(context);
                    ttSdk = new TiktokSdk(ttConfig);

                    // start tracking
                    context.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

                        }

                        @Override
                        public void onActivityStarted(@NonNull Activity activity) {

                        }

                        @Override
                        public void onActivityResumed(@NonNull Activity activity) {

                        }

                        @Override
                        public void onActivityPaused(@NonNull Activity activity) {
                            TTAppEventStorage.persist();
                        }

                        @Override
                        public void onActivityStopped(@NonNull Activity activity) {
                            TTAppEventStorage.persist();
                        }

                        @Override
                        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

                        }

                        @Override
                        public void onActivityDestroyed(@NonNull Activity activity) {

                        }
                    });
                    // restart tracking
                }
            }
        }
        return ttSdk;
    }

    // public functions like track ...
}
