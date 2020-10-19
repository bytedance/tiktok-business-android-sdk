package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;
import com.tiktok.util.TimeUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

class TTRequest {
    private static String TAG = TTRequest.class.getCanonicalName();
    private static TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private static final int MAX_EVENT_SIZE = 50;

    // stats for the current batch
    private static int toBeSentRequests = 0;
    private static int failedRequests = 0;
    private static int successfulRequests = 0;

    // stats for the whole lifecycle
    private static TreeSet<Long> allRequestIds = new TreeSet<Long>();
    private static List<TTAppEvent> successfullySentRequests = new ArrayList<>();

    private static Map<String, String> headParamMap = new HashMap<>();

    static {
        // these fields wont change, so cache it locally to enhance performance
        headParamMap.put("Content-Type", "application/json");
        headParamMap.put("Connection", "Keep-Alive");
        headParamMap.put("User-Agent", "tiktok-business-android-sdk/1.0.0");
        //TODO BOE env need config x-tt-env, remove after going online
        headParamMap.put("x-tt-env", "jianyi");
    }

    public static JSONObject getBusinessSDKConfig() {
        logger.info("Try to fetch global configs");
        headParamMap.put("access-token", TiktokBusinessSdk.getAccessToken());
//        String url = "https://ads.tiktok.com/open_api/business_sdk_config/get/?app_id="+TiktokBusinessSdk.getAppId();
        String url = "http://10.231.18.90:9351/open_api/v1.1/business_sdk_config/get/?app_id="+TiktokBusinessSdk.getAppId();
        String result = HttpRequestUtil.doGet(url, headParamMap);
        logger.verbose(result);
        JSONObject config = null;
        if (result != null) {
            try {
                JSONObject resultJson = new JSONObject(result);
                Integer code = (Integer) resultJson.get("code");
                if (code == 0) {
                    config = (JSONObject) resultJson.get("data");
                }
                logger.info("Global config fetched: " + config.toString());
            } catch (Exception e) {
                TTCrashHandler.handleCrash(TAG, e);
            }
        }

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
    public static synchronized List<TTAppEvent> appEventReport(JSONObject basePayload, List<TTAppEvent> appEventList) {
        TTUtil.checkThread(TAG);
        // access-token might change during runtime
        headParamMap.put("access-token", TiktokBusinessSdk.getAccessToken());

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

        String url = "https://ads.tiktok.com/open_api/v1.1/app/batch/";

        List<TTAppEvent> failedEvents = new ArrayList<>();

        List<List<TTAppEvent>> chunks = averageAssign(appEventList, MAX_EVENT_SIZE);

        for (List<TTAppEvent> currentBatch : chunks) {
            List<JSONObject> batch = new ArrayList<>();
            for (TTAppEvent event : currentBatch) {
                JSONObject jsonObject = transferJson(event);
                if (jsonObject == null) {
                    continue;
                }
                batch.add(jsonObject);
            }

            JSONObject bodyJson = basePayload;
            try {
                bodyJson.put("batch", batch);
            } catch (Exception e) {
                failedEvents.addAll(currentBatch);
                TTCrashHandler.handleCrash(TAG, e);
                continue;
            }

            String bodyStr = bodyJson.toString();
            logger.verbose("To Api:\n" + bodyStr);

            String result = HttpRequestUtil.doPost(url, headParamMap, bodyJson.toString());

            if (result == null) {
                failedEvents.addAll(currentBatch);
                failedRequests += currentBatch.size();
            } else {
                try {
                    JSONObject resultJson = new JSONObject(result);
                    Integer code = (Integer) resultJson.get("code");

                    if (code != 0) {
                        failedEvents.addAll(currentBatch);
                        failedRequests += currentBatch.size();
                    } else {
                        successfulRequests += currentBatch.size();
                        successfullySentRequests.addAll(currentBatch);
                    }
                } catch (JSONException e) {
                    failedRequests += currentBatch.size();
                    failedEvents.addAll(currentBatch);
                    TTCrashHandler.handleCrash(TAG, e);
                }
                logger.verbose(result);
            }
            notifyChange();
        }
        logger.verbose("Flushed %d events, failed to flush %d events", successfulRequests, failedEvents.size());
        toBeSentRequests = 0;
        failedRequests = 0;
        successfulRequests = 0;
        notifyChange();
        return failedEvents;
    }

    private static void notifyChange() {
        if (TiktokBusinessSdk.networkListener != null) {
            TiktokBusinessSdk.networkListener.onNetworkChange(toBeSentRequests, successfulRequests,
                    failedRequests, allRequestIds.size() + TTAppEventsQueue.size(), successfullySentRequests.size());
        }
    }

    private static JSONObject transferJson(TTAppEvent event) {
        if (event == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "track");
            jsonObject.put("event", event.getEventName());
            //TODO 转化时间的时候用默认时区吗？
            jsonObject.put("timestamp", TimeUtil.getISO8601Timestamp(event.getTimeStamp()));
            jsonObject.put("properties", event.getJsonObject());
            return jsonObject;
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e);
            return null;
        }
    }

    /**
     * 切分列表
     *
     * @param sourceList 列表
     * @param splitNum   切分份数
     * @param <T>
     */
    public static <T> List<List<T>> averageAssign(List<T> sourceList, int splitNum) {
        List<List<T>> result = new ArrayList<>();

        int size = sourceList.size();
        int times = size % splitNum == 0 ? size / splitNum : size / splitNum + 1;
        for (int i = 0; i < times; i++) {
            int start = i * splitNum;
            int end = i * splitNum + splitNum;
            result.add(new ArrayList<>(sourceList.subList(i * splitNum, Math.min(size, end))));
        }
        return result;
    }
}
