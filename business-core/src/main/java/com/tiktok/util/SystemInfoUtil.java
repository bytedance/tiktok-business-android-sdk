/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.util;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.WebSettings;

import android.webkit.WebView;
import androidx.annotation.RequiresApi;
import com.tiktok.BuildConfig;
import com.tiktok.TikTokBusinessSdk;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class SystemInfoUtil {

    static PackageManager pm;
    static PackageInfo packageInfo;
    static Application application;

    static {
        try {
            application = TikTokBusinessSdk.getApplicationContext();
            pm = application.getPackageManager();
            packageInfo = pm.getPackageInfo(TikTokBusinessSdk.getApplicationContext().getPackageName(), 0);
        } catch (Exception ignored) {
        }
    }

    public static String getPackageName() {
        return packageInfo.packageName;
    }

    public static String getAppName() {
        return application.getApplicationInfo().loadLabel(pm).toString();
    }

    public static String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getAppVersionName() {
        if (packageInfo == null) {
            return "";
        }
        return packageInfo.versionName;
    }

    public static int getAppVersionCode() {
        if (packageInfo == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return (int) packageInfo.getLongVersionCode();
        }
        // noinspection deprecation
        return packageInfo.versionCode;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            return "";
        }
        return "";
    }

    public static String getLocale() {
        return Locale.getDefault().getLanguage();
    }

    private static String userAgent = "";

    // invoke it in the mainThread
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void initUserAgent() {
        try {
            userAgent = WebSettings.getDefaultUserAgent(TikTokBusinessSdk.getApplicationContext());
        } catch (Exception e) {
            userAgent = System.getProperty("http.agent");
        }
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.SDK_INT + "";
    }

}
