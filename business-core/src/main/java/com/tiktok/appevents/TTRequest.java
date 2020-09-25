package com.tiktok.appevents;

import android.util.Log;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.HttpRequestUtil;
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

    public static List<TTAppEvent>  appEventReport(List<TTAppEvent> appEventList, String appId, JSONObject context) {

        if(appEventList == null || appEventList.size() == 0) {
            return new ArrayList<>();
        }

        //TODO context 如何获取?
        String url = "http://10.231.18.38:9496/open_api/2/app/batch/";

        Map<String, String> headParamMap = new HashMap<>();
        headParamMap.put("Content-Type", "application/json");
        headParamMap.put("Connection", "Keep-Alive");
        headParamMap.put("access-token", TiktokBusinessSdk.getAccessToken());
        headParamMap.put("x-tt-env", "jianyi");

        JSONObject bodyJson = new JSONObject();

        List<TTAppEvent> failedEvents = new ArrayList<>();

        List<List<TTAppEvent>> batchList = averageAssign(appEventList, countSplitNum(appEventList.size(), MAX_EVENT_SIZE));

        for(List<TTAppEvent> eventList: batchList) {
            try {
                List<JSONObject> batch = new ArrayList<>();
                for (TTAppEvent event : eventList) {
                    JSONObject jsonObject = transferJson(event);
                    if(jsonObject == null){
                        continue;
                    }
                    batch.add(jsonObject);
                }

                bodyJson.put("app_id", appId);
                bodyJson.put("context", context);
                bodyJson.put("batch", batch);

                Log.i(TAG, bodyJson.toString());

                String result = HttpRequestUtil.doPost(url, headParamMap, bodyJson.toString());

                if (result == null) {
                    failedEvents.addAll(eventList);
                }else {
                    try {
                        JSONObject resultJson = new JSONObject(result);

                        Integer code = (Integer) resultJson.get("code");

                        if (code == null || code != 0) {
                            failedEvents.addAll(eventList);
                        }
                    } catch (JSONException e) {
                        failedEvents.addAll(eventList);
                        e.printStackTrace();
                    }
                    Log.i(TAG, result);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return failedEvents;
    }

    private static JSONObject transferJson(TTAppEvent event) throws JSONException {
        if (event == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "track");
        jsonObject.put("event", event.getEventName());
        //TODO 转化时间的时候用默认时区吗？
        jsonObject.put("timestamp", TimeUtil.getISO8601Timestamp(event.getTimeStamp()));
        jsonObject.put("properties", event.getJsonObject());
        //TODO context 如何获取?
        jsonObject.put("context", "123");

        return jsonObject;
    }


    /**
     * 计算切分份数
     * @param totalCount 总数
     * @param partition 每个子列表的数量
     * @return
     */
    public static int countSplitNum(int totalCount, int partition) {
        return (totalCount + partition-1)/partition;
    }

    /**
     * 切分列表
     * @param sourceList 列表
     * @param splitNum 切分份数
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> averageAssign(List<T> sourceList, int splitNum) {
        List<List<T>> result = new ArrayList<>();

        //余数
        int remainder = sourceList.size()%splitNum;
        //商
        int number = sourceList.size()/splitNum;
        //偏移
        int offset = 0;

        for(int i=0; i<splitNum; i++) {
            List<T> list;
            if(remainder >0) {
                list = sourceList.subList(i * number + offset, (i + 1) * number +  offset + 1);
                remainder--;
                offset++;
            }else {
                list = sourceList.subList(i * number + offset, (i + 1) * number +  offset);
            }
            result.add(list);
        }

        return result;
    }
}
