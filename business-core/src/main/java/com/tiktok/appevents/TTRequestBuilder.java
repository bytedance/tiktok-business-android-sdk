package com.tiktok.appevents;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

class TTRequestBuilder {
    private static final String TAG = TTRequestBuilder.class.getCanonicalName();

    private static JSONObject basePayloadCache = null;

    public static JSONObject getBasePayload(Context context, boolean isGaidCollectionEnabled) {
        TTUtil.checkThread(TAG);
        if (basePayloadCache != null) {
            return basePayloadCache;
        }
        JSONObject result = new JSONObject();

        try {
            result.put("app_id", TiktokBusinessSdk.getAppId());
            result.put("locale", getBcp47Language());
            result.put("ip", SystemInfoUtil.getLocalIpAddress());

            String userAgent = SystemInfoUtil.getUserAgent();
            if (userAgent != null) {
                result.put("user_agent", userAgent);
            }

            TTIdentifierFactory.AdIdInfo adIdInfo = null;
            try {
                if (isGaidCollectionEnabled) {
                    adIdInfo = TTIdentifierFactory.getGoogleAdIdInfo(context);
                }
            } catch (Exception e) {
                TTCrashHandler.handleCrash(TAG, e);
            }
            result.put("context", contextBuilder(adIdInfo));
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
            basePayloadCache = new JSONObject();
            return basePayloadCache;
        }
        basePayloadCache = result;
        return result;
    }

    private static Locale getCurrentLocale() {
        Context context = TiktokBusinessSdk.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            // noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    private static String getBcp47Language() {
        Locale loc = getCurrentLocale();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return loc.toLanguageTag();
        }

        // we will use a dash as per BCP 47
        final char SEP = '-';
        String language = loc.getLanguage();
        String region = loc.getCountry();
        String variant = loc.getVariant();

        // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
        // this goes before the string matching since "NY" wont pass the variant checks
        if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            language = "nn";
            region = "NO";
            variant = "";
        }

        if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
            language = "und";       // Follow the Locale#toLanguageTag() implementation
            // which says to return "und" for Undetermined
        } else if (language.equals("iw")) {
            language = "he";        // correct deprecated "Hebrew"
        } else if (language.equals("in")) {
            language = "id";        // correct deprecated "Indonesian"
        } else if (language.equals("ji")) {
            language = "yi";        // correct deprecated "Yiddish"
        }

        // ensure valid country code, if not well formed, it's omitted
        if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
            region = "";
        }

        // variant subtags that begin with a letter must be at least 5 characters long
        if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
            variant = "";
        }

        StringBuilder bcp47Tag = new StringBuilder(language);
        if (!region.isEmpty()) {
            bcp47Tag.append(SEP).append(region);
        }
        if (!variant.isEmpty()) {
            bcp47Tag.append(SEP).append(variant);
        }

        return bcp47Tag.toString();
    }

    private static JSONObject contextBuilder(@Nullable TTIdentifierFactory.AdIdInfo adIdInfo) throws JSONException {
        JSONObject app = new JSONObject();
        app.put("name", SystemInfoUtil.getAppName());
        app.put("namespace", SystemInfoUtil.getPackageName());
        app.put("version", SystemInfoUtil.getVersionName());
        app.put("build", SystemInfoUtil.getVersionCode());

        JSONObject device = new JSONObject();
        device.put("platform", "Android");
        if (adIdInfo != null) {
            device.put("gaid", adIdInfo.getAdId());
        }
        JSONObject context = new JSONObject();
        context.put("app", app);
        context.put("device", device);
        return context;
    }
}