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

    private AtomicBoolean isInitialEventsLogged = new AtomicBoolean(false);
    // for test purpose
    static final long TWO_SECONDS = 2 * 1000;
    private static final long TWO_DAYS = 48 * 60 * 60 * 1000;
    private static SimpleDateFormat fm;

    static {
        fm = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        fm.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final TTAppEventLogger appEventLogger;

    public TTActivityLifecycleCallbacksListener(TTAppEventLogger appEventLogger) {
        this.appEventLogger = appEventLogger;
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        // App created, only called once per app lifecycle
        if (shouldTrackAppLifecycleEvents() && !isInitialEventsLogged.getAndSet(true)) {
            trackFirstInstallEvent();
            track2DayRetentionEvent();
            appEventLogger.track("LaunchApp", null);
        }
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        appEventLogger.startScheduler();
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        appEventLogger.stopScheduler();
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

    private boolean shouldTrackAppLifecycleEvents() {
        return appEventLogger.lifecycleTrackEnable;
    }

    private void trackFirstInstallEvent() {
        /* gets current app version & build */
        String currentVersion = appEventLogger.getVersionName();
        String currentBuild = String.valueOf(appEventLogger.getVersionCode());

        /* get the previous recorded version. */
        String previousVersion = appEventLogger.store.get(TTSDK_APP_VERSION);
        String previousBuild = appEventLogger.store.get(TTSDK_APP_BUILD);

        /* check and track InstallApp. */
        if (previousBuild == null) {
            appEventLogger.track("InstallApp", null);
            appEventLogger.store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
        } else if (!currentBuild.equals(previousBuild)) {
            // app updated
        }

        /* update store with existing version. */
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(TTSDK_APP_VERSION, currentVersion);
        hm.put(TTSDK_APP_BUILD, currentBuild);
        appEventLogger.store.set(hm);
    }

    // extract into a single method to simplify writing unit test
    private boolean isSatisfyRetention(long duration) {
        String isLogged = appEventLogger.store.get(TTConst.TTSDK_APP_2DRENTION_LOGGED);
        if (isLogged != null && isLogged.equals("true")) {
            return false;
        }
        // check 2Dretention
        Date now = new Date();
        String dateFromStore = appEventLogger.store.get(TTConst.TTSDK_APP_LAST_LAUNCH);
        if (dateFromStore == null) {
            logger.warn("First Launch Date should already been set in the trackFirstInstallEvent, could be a bug");
            appEventLogger.store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
            return false;
        }
        try {
            long beforeTime = fm.parse(dateFromStore).getTime();
            return now.getTime() - beforeTime >= duration;
        } catch (Exception e) {
            logger.info("Failed to check 2day retention %s", e.getMessage());
            // if failed to parse the date, try to set it to now
            appEventLogger.store.set(TTConst.TTSDK_APP_LAST_LAUNCH, fm.format(new Date()));
            return false;
        }
    }

    private void track2DayRetentionEvent() {
        if (isSatisfyRetention(TWO_DAYS)) {
            appEventLogger.track("2Dretention", null);
            appEventLogger.store.set(TTConst.TTSDK_APP_2DRENTION_LOGGED, "true");
        }
    }

}

