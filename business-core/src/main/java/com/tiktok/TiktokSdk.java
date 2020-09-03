package com.tiktok;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class TiktokSdk {
    private static final String TAG = TiktokSdk.class.getName();

    private static volatile TiktokSdk ttSdk = null;

    private TiktokSdk(TTConfig conf) {

    }

    public static synchronized void initialize(TTConfig ttConfig) {
        if (ttSdk == null) {
            ttSdk = new TiktokSdk(ttConfig);
        }
        // start tracking
    }

    public static class TTConfig {
        private final Context context;
        private static String appKey;
        private static boolean debug = false;
        private static boolean autoEventTrackEnable = true;
        private static boolean advertiserIDCollectionEnable = true;

        public TTConfig(Context ctx) {
            this.context = ctx;
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

    public static TiktokSdk using(Context context) {
        if (ttSdk == null) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            synchronized (TiktokSdk.class) {
                if (ttSdk == null) {
                    TTConfig ttConfig = new TTConfig(context);
                    ttSdk = new TiktokSdk(ttConfig);
                    // restart tracking
                }
            }
        }
        return ttSdk;
    }

    // public functions like track ...
}
