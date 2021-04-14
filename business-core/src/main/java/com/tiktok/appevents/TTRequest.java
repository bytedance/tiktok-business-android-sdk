/*******************************************************************************
 * Copyright (c) 2020. Bytedance Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents;

import com.tiktok.BuildConfig;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.SystemInfoUtil;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import com.tiktok.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class TTRequest {
    private static final String TAG = TTRequest.class.getCanonicalName();
    private static final TTLogger logger = new TTLogger(TAG, TikTokBusinessSdk.getLogLevel());

    private static final int MAX_EVENT_SIZE = 50;

    // stats for the current batch
    private static int toBeSentRequests = 0;
    private static int failedRequests = 0;
    private static int successfulRequests = 0;

    // stats for the whole lifecycle
    private static final TreeSet<Long> allRequestIds = new TreeSet<>();
    private static final List<TTAppEvent> successfullySentRequests = new ArrayList<>();

    private static final Map<String, String> headParamMap = new HashMap<>();
    private static final Map<String, String> getHeadParamMap = new HashMap<>();

    static {
        // these fields wont change, so cache it locally to enhance performance
        headParamMap.put("Content-Type", "application/json");
        headParamMap.put("Connection", "Keep-Alive");
//        headParamMap.put("X-USE-PPE", "1");
//        headParamMap.put("X-TT-ENV", "ppe_opensite_i18n_m");

        String ua = String.format("tiktok-business-android-sdk/%s/%s",
                BuildConfig.VERSION_NAME,
                TikTokBusinessSdk.getApiAvailableVersion());
        headParamMap.put("User-Agent", ua);
        // no content-type application/json for get requests
        getHeadParamMap.put("Connection", "Keep-Alive");
        getHeadParamMap.put("User-Agent", ua);
    }

    public static JSONObject getBusinessSDKConfig(Map<String, Object> options) {
        logger.info("Try to fetch global configs");
        getHeadParamMap.put("access-token", TikTokBusinessSdk.getAccessToken());
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("app_id", TikTokBusinessSdk.getAppId());
        // the rest params are for the sake of simplicity of debugging
        paramsMap.put("client", "android");
        paramsMap.put("sdk_version", SystemInfoUtil.getSDKVersion());
        paramsMap.put("app_name", SystemInfoUtil.getAppName());
        paramsMap.put("app_version", SystemInfoUtil.getAppVersionName());
        paramsMap.put("tt_app_id", TikTokBusinessSdk.getTTAppId());
        paramsMap.putAll(options);

        String url = "https://ads.tiktok.com/open_api/business_sdk_config/get/?" + TTUtil.mapToString(paramsMap, "&");
        logger.debug(url);
        String result = HttpRequestUtil.doGet(url, getHeadParamMap);
        logger.debug(result);
        JSONObject config = null;
        if (result != null) {
            try {
                JSONObject resultJson = new JSONObject(result);
                Integer code = (Integer) resultJson.get("code");
                if (code == 0) {
                    config = (JSONObject) resultJson.get("data");
                }
                logger.info("Global config fetched: " + TTUtil.ppStr(config));
            } catch (Exception e) {
                // might be api returning something wrong
                TTCrashHandler.handleCrash(TAG, e);
            }
        }

        // might be api returning something wrong
        return config;
    }

    // for debugging purpose
    public static synchronized List<TTAppEvent> getSuccessfullySentRequests() {
        return successfullySentRequests;
    }

    /**
     * Try to send events to api with MTU set to 1000 app events,
     * If there are more than 1000 events, they will be split into several chunks and
     * then be sent separately,
     * Any failed events will be accumulated and finally returned.
     *
     * @param appEventList
     * @return the accumulation of all failed events
     */
    public static synchronized List<TTAppEvent> reportAppEvent(JSONObject basePayload, List<TTAppEvent> appEventList) {
        TTUtil.checkThread(TAG);
        // access-token might change during runtime
        headParamMap.put("access-token", TikTokBusinessSdk.getAccessToken());

        if (appEventList == null || appEventList.size() == 0) {
            return new ArrayList<>();
        }

        toBeSentRequests = appEventList.size();
        for (TTAppEvent event : appEventList) {
            allRequestIds.add(event.getUniqueId());
        }
        failedRequests = 0;
        successfulRequests = 0;
        notifyChange();

        String url = "https://ads.tiktok.com/open_api/" + TikTokBusinessSdk.getApiAvailableVersion() + "/app/batch/";
//        String url = "http://10.231.253.20:6789/open_api/" + TikTokBusinessSdk.getApiAvailableVersion() + "/app/batch/";

        List<TTAppEvent> failedEventsToBeSaved = new ArrayList<>();
        List<TTAppEvent> failedEventsToBeDiscarded = new ArrayList<>();

        List<List<TTAppEvent>> chunks = averageAssign(appEventList, MAX_EVENT_SIZE);

        for (List<TTAppEvent> currentBatch : chunks) {
            List<JSONObject> batch = new ArrayList<>();
            for (TTAppEvent event : currentBatch) {
                JSONObject propertiesJson = transferJson(event);
                if (propertiesJson == null) {
                    continue;
                }
                batch.add(propertiesJson);
            }

            JSONObject bodyJson = basePayload;
            try {
                bodyJson.put("batch", new JSONArray(batch));
            } catch (Exception e) {
                failedEventsToBeSaved.addAll(currentBatch);
                TTCrashHandler.handleCrash(TAG, e);
                continue;
            }

            try {
                String bodyStr = bodyJson.toString(4);
                logger.debug("To Api:\n" + bodyStr);
            } catch (JSONException e) {
            }

            String result = HttpRequestUtil.doPost(url, headParamMap, bodyJson.toString());

            if (result == null) {
                failedEventsToBeSaved.addAll(currentBatch);
                failedRequests += currentBatch.size();
            } else {
                try {
                    JSONObject resultJson = new JSONObject(result);
                    int code = (Integer) resultJson.getInt("code");

                    if (code == TTConst.ApiErrorCodes.API_ERROR.code) {
                        failedEventsToBeDiscarded.addAll(currentBatch);
                        failedRequests += currentBatch.size();
                    }
                    // some events made it while others not.
                    else if (code == TTConst.ApiErrorCodes.PARTIAL_SUCCESS.code) {
                        try {
                            JSONArray partialFailedEvents = resultJson.getJSONObject("data").getJSONArray("failed_events");
                            int length = partialFailedEvents.length();
                            Set<Integer> failedIndices = new HashSet<>();
                            for (int i = 0; i < length; i++) {
                                JSONObject errorObj = partialFailedEvents.getJSONObject(i);
                                failedIndices.add(errorObj.getInt("order_in_batch"));
                            }
                            int totalSize = currentBatch.size();
                            for (int i = 0; i < totalSize; i++) {
                                TTAppEvent curr = currentBatch.get(i);
                                if (failedIndices.contains(i)) {
                                    failedEventsToBeDiscarded.add(curr);
                                    failedRequests += 1;
                                } else {
                                    successfullySentRequests.add(curr);
                                    successfulRequests += 1;
                                }
                            }
                        } catch (Exception e) {
                            TTCrashHandler.handleCrash(TAG, e);
                            failedEventsToBeSaved.addAll(currentBatch);
                            failedRequests += currentBatch.size();
                        }
                    } else if (code != 0) {
                        failedEventsToBeSaved.addAll(currentBatch);
                        failedRequests += currentBatch.size();
                    } else {
                        successfulRequests += currentBatch.size();
                        successfullySentRequests.addAll(currentBatch);
                    }
                } catch (JSONException e) {
                    failedRequests += currentBatch.size();
                    failedEventsToBeSaved.addAll(currentBatch);
                    TTCrashHandler.handleCrash(TAG, e);
                }
                logger.debug(TTUtil.ppStr(result));
            }
            notifyChange();
        }
        logger.debug("Flushed %d events successfully", successfulRequests);

        // might be due to network disconnection
        if (failedEventsToBeSaved.size() != 0) {
            logger.debug("Failed to flush %d events, will save them to disk", failedEventsToBeSaved.size());
        }
        // api returns some unrecoverable error
        int discardedEventCount = failedEventsToBeDiscarded.size();
        if (discardedEventCount != 0) {
            logger.debug("Failed to flush " + discardedEventCount + " events, will discard them");
            TTAppEventLogger.totalDumped += discardedEventCount;
            TikTokBusinessSdk.diskListener.onDumped(TTAppEventLogger.totalDumped);
        }
        logger.debug("Failed to flush %d events in total", failedRequests);

        toBeSentRequests = 0;
        failedRequests = 0;
        successfulRequests = 0;
        notifyChange();
        return failedEventsToBeSaved;
    }

    private static void notifyChange() {
        if (TikTokBusinessSdk.networkListener != null) {
            TikTokBusinessSdk.networkListener.onNetworkChange(toBeSentRequests, successfulRequests,
                    failedRequests, allRequestIds.size() + TTAppEventsQueue.size(), successfullySentRequests.size());
        }
    }

    private static JSONObject transferJson(TTAppEvent event) {
        if (event == null) {
            return null;
        }
        try {
            JSONObject propertiesJson = new JSONObject();
            propertiesJson.put("type", event.getType());
            if (event.getEventName() != null) {
                propertiesJson.put("event", event.getEventName());
            }
            propertiesJson.put("timestamp", TimeUtil.getISO8601Timestamp(event.getTimeStamp()));
            JSONObject properties = new JSONObject(event.getPropertiesJson());
            if (properties.length() != 0) {
                propertiesJson.put("properties", properties);
            }
            propertiesJson.put("context", TTRequestBuilder.getContextForApi(event));
            return propertiesJson;
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e);
            return null;
        }
    }

    /**
     * split event list
     *
     * @param sourceList
     * @param splitNum
     * @param <T>
     */
    public static <T> List<List<T>> averageAssign(List<T> sourceList, int splitNum) {
        List<List<T>> result = new ArrayList<>();

        int size = sourceList.size();
        int times = size % splitNum == 0 ? size / splitNum : size / splitNum + 1;
        for (int i = 0; i < times; i++) {
            int start = i * splitNum;
            int end = i * splitNum + splitNum;
            result.add(new ArrayList<>(sourceList.subList(start, Math.min(size, end))));
        }
        return result;
    }
}