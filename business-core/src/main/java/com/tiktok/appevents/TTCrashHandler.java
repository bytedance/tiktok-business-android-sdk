/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import android.content.Context;

import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A global crash handler which mainly does
 * 1. send error details to a remote analytic tool
 * 2. Prevent from app from crash
 */
public class TTCrashHandler {
    private static final String TAG = TTCrashHandler.class.getCanonicalName();
    private static TTLogger ttLogger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());
    private static final String EVENT_CRASH_REPORT = "crash-log.json";
    private static final String TTSDK_PREFIX = "com.tiktok";
    private static final String TTSDKCrashInfo = "crash_info";
    private static final String TTSDKCrashReportID = "crash_log_id";
    private static final String TTSDKCrashSDKVeriosn = "crash_sdk_version";
    private static final String TTSDKCrashTimestamp = "timestamp";
    private static final String TTSDKVersion = "TikTok SDK Version";

    private static volatile TTCrashHandler instance = new TTCrashHandler();

    public static void handleCrash(String originTag, Throwable t) {
        ttLogger.error(t, "Error caused by sdk at " + originTag + "\n" + t.getMessage() + "\n"
                + getStackTrace(t)
        );
    }

    public static void handleUncaughtCrash(String originTag, Throwable t) {
        ttLogger.error(t, "Error caused by sdk at " + originTag + "\n" + t.getMessage() + "\n"
                + getStackTrace(t)
        );
        if (shouldSaveCrashReport(t)) {
            saveCrash(t);
        }
    }

    private static boolean shouldSaveCrashReport(Throwable t) {
        for (StackTraceElement element :t.getStackTrace()) {
            if(element.getClassName().startsWith(TTSDK_PREFIX)) {
                return true;
            }
        }

        for (StackTraceElement element :t.getCause().getStackTrace()) {
            if(element.getClassName().startsWith(TTSDK_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private static String getStackTrace(Throwable t) {
        StringBuffer buffer = new StringBuffer();
        for (StackTraceElement curr : t.getStackTrace()) {
            buffer.append(curr.toString() + "\n");
        }
        return buffer.toString();
    }

    private static boolean saveCrash(Throwable t) {
        Context context = TikTokBusinessSdk.getApplicationContext();
        deleteCrashReport();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(context.openFileOutput(EVENT_CRASH_REPORT, Context.MODE_PRIVATE)))) {
            Map<String, String> crashReport = new HashMap<>();
            String crashReportId = UUID.randomUUID().toString();
            String sdkVersion = SystemInfoUtil.getSDKVersion();
            String crashInfo =TTSDKVersion + ":" + sdkVersion + "\n" + getStackTrace(t) + "\n" + getStackTrace(t.getCause());
            crashReport.put(TTSDKCrashReportID, crashReportId);
            crashReport.put(TTSDKCrashSDKVeriosn, sdkVersion);
            crashReport.put(TTSDKCrashInfo, crashInfo);
            crashReport.put(TTSDKCrashTimestamp, String.valueOf(System.currentTimeMillis()/1000));

            oos.writeObject(crashReport);
            return true;
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
            return false;
        }
    }

    public static void deleteCrashReport() {
        Context context = TikTokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_CRASH_REPORT);
        if (f.exists()) {
            f.delete();
        }
    }

    public static Map<String, String> readCrashReport() {
        TTUtil.checkThread(TAG);
        Context context = TikTokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_CRASH_REPORT);
        if (!f.exists()) {
            return null;
        }
        Map<String, String> crashReport = null;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(context.openFileInput(EVENT_CRASH_REPORT)))) {
            crashReport = (Map<String, String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            TTCrashHandler.handleCrash(TAG, e);
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
        return crashReport;
    }

    public static void sendCrashReport() {
        TTUtil.checkThread(TAG);
        Map<String, String> crashReport = readCrashReport();
        if (crashReport == null) return;
        JSONObject jsonBody = TTRequestBuilder.getBasePayload();
        try {
            JSONObject crashLog = new JSONObject();
            for(String key: crashReport.keySet()) {
                crashLog.put(key, crashReport.get(key));
            }
            jsonBody.put("crash_report", crashLog);

            String crashLogId = TTRequest.sendCrashReport(jsonBody);
            if (crashLogId == null) { // send report failed
                ttLogger.debug("Failed to send crash report");
            } else {
                deleteCrashReport();
            }
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e);
        }
    }
}
