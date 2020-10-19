package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTKeyValueStore;
import com.tiktok.util.TTLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tiktok.util.TTConst.TTSDK_APP_BUILD;
import static com.tiktok.util.TTConst.TTSDK_APP_VERSION;

class TTAutoEventsManager {

    // for test purpose
    static final long TWO_SECONDS = 2 * 1000;
    private static final long TWO_DAYS = 48 * 60 * 60 * 1000;
    private static SimpleDateFormat fm;
    private TTLogger logger;
    private static final String TAG = TTAutoEventsManager.class.getCanonicalName();

    static {
        fm = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        fm.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private TTAppEventLogger appEventLogger;
    private TTKeyValueStore store;

    public TTAutoEventsManager(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
        store = new TTKeyValueStore(TiktokBusinessSdk.getApplicationContext());
        logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
    }

    private boolean shouldTrackAppLifecycleEvents() {
        return appEventLogger.lifecycleTrackEnable;
    }

    /**
     * the events to be tracked when the app was just activated
     * 1. firstInstall
     * 2. 2Dretention
     * 3. launchApp
     */
    public void trackOnAppOpenEvents() {
        if (shouldTrackAppLifecycleEvents()) {
            trackFirstInstallEvent();
            track2DayRetentionEvent();
            appEventLogger.track("LaunchApp", null);
        }
    }

    private void trackFirstInstallEvent() {
        /* gets current app version & build */
        String currentVersion = SystemInfoUtil.getVersionName();
        String currentBuild = String.valueOf(SystemInfoUtil.getVersionCode());

        /* get the previous recorded version. */
        String previousVersion = store.get(TTSDK_APP_VERSION);
        String previousBuild = store.get(TTSDK_APP_BUILD);

        /* check and track InstallApp. */
        if (previousBuild == null) {
            appEventLogger.track("InstallApp", null);
            store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
        } else if (!currentBuild.equals(previousBuild)) {
            // app updated
        }

        /* update store with existing version. */
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(TTSDK_APP_VERSION, currentVersion);
        hm.put(TTSDK_APP_BUILD, currentBuild);
        store.set(hm);
    }

    // extract into a single method to simplify writing unit test
    private boolean isSatisfyRetention(long duration) {
        String isLogged = store.get(TTConst.TTSDK_APP_2DRENTION_LOGGED);
        if (isLogged != null && isLogged.equals("true")) {
            return false;
        }
        // check 2Dretention
        Date now = new Date();
        String dateFromStore = store.get(TTConst.TTSDK_APP_LAST_LAUNCH);
        if (dateFromStore == null) {
            logger.warn("First Launch Date should already been set in the trackFirstInstallEvent, could be a bug");
            store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
            return false;
        }
        try {
            long beforeTime = fm.parse(dateFromStore).getTime();
            return now.getTime() - beforeTime >= duration;
        } catch (Exception e) {
            logger.info("Failed to check 2day retention %s", e.getMessage());
            // if failed to parse the date, try to set it to now
            store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
            return false;
        }
    }

    private void track2DayRetentionEvent() {
        if (isSatisfyRetention(TWO_DAYS)) {
            appEventLogger.track("2Dretention", null);
            store.set(TTConst.TTSDK_APP_2DRENTION_LOGGED, "true");
        }
    }

}
