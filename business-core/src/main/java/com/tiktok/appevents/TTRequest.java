package com.tiktok.appevents;

import android.util.Log;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TimeUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TTRequest {
    private static String TAG = TTRequest.class.getCanonicalName();

    private static final int MAX_EVENT_SIZE = 1000;

    private static Map<String, String> headParamMap = new HashMap<>();

    static {
        // these fields wont change, so cache it locally to enhance performance
        headParamMap.put("Content-Type", "application/json");
        headParamMap.put("Connection", "Keep-Alive");
        headParamMap.put("x-tt-env", "jianyi");
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
    public static List<TTAppEvent> appEventReport(List<TTAppEvent> appEventList) {
        // access-token might change during runtime
        headParamMap.put("access-token", TiktokBusinessSdk.getAccessToken());

        if (appEventList == null || appEventList.size() == 0) {
            return new ArrayList<>();
        }

        TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());
        String appId = TiktokBusinessSdk.getAppId();
        JSONObject context = new JSONObject();  //TODO context 如何获取?
        String url = "http://10.231.18.38:9496/open_api/2/app/batch/";


        JSONObject bodyJson = new JSONObject();

        List<TTAppEvent> failedEvents = new ArrayList<>();

        List<List<TTAppEvent>> chunks = averageAssign(appEventList, countSplitNum(appEventList.size(), MAX_EVENT_SIZE));

        for (List<TTAppEvent> currentBatch : chunks) {
            List<JSONObject> batch = new ArrayList<>();
            for (TTAppEvent event : currentBatch) {
                JSONObject jsonObject = transferJson(event);
                if (jsonObject == null) {
                    continue;
                }
                batch.add(jsonObject);
            }

            try {
                bodyJson.put("app_id", appId);
                bodyJson.put("context", context);
                bodyJson.put("batch", batch);
            } catch (Exception e) {
                failedEvents.addAll(currentBatch);
                TTCrashHandler.handleCrash(TAG, e);
                continue;
            }

            logger.verbose("To Api:\n" + bodyJson.toString());

            String result = HttpRequestUtil.doPost(url, headParamMap, bodyJson.toString());

            if (result == null) {
                failedEvents.addAll(currentBatch);
            } else {
                try {
                    JSONObject resultJson = new JSONObject(result);

                    Integer code = (Integer) resultJson.get("code");

                    if (code != 0) {
                        failedEvents.addAll(currentBatch);
                    }
                } catch (JSONException e) {
                    failedEvents.addAll(currentBatch);
                    TTCrashHandler.handleCrash(TAG, e);
                }
                logger.verbose(result);
            }

        }

        return failedEvents;
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
            //TODO context 如何获取?
            jsonObject.put("context", "123");
            return jsonObject;
        } catch (JSONException e) {
            TTCrashHandler.handleCrash(TAG, e);
            return null;
        }
    }


    /**
     * 计算切分份数
     *
     * @param totalCount 总数
     * @param partition  每个子列表的数量
     * @return
     */
    public static int countSplitNum(int totalCount, int partition) {
        return (totalCount + partition - 1) / partition;
    }

    /**
     * 切分列表
     *
     * @param sourceList 列表
     * @param splitNum   切分份数
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> averageAssign(List<T> sourceList, int splitNum) {
        List<List<T>> result = new ArrayList<>();

        //余数
        int remainder = sourceList.size() % splitNum;
        //商
        int number = sourceList.size() / splitNum;
        //偏移
        int offset = 0;

        for (int i = 0; i < splitNum; i++) {
            List<T> list;
            if (remainder > 0) {
                list = new ArrayList<>(sourceList.subList(i * number + offset, (i + 1) * number + offset + 1));
                remainder--;
                offset++;
            } else {
                list = new ArrayList<>(sourceList.subList(i * number + offset, (i + 1) * number + offset));
            }
            result.add(list);
        }

        return result;
    }
}
