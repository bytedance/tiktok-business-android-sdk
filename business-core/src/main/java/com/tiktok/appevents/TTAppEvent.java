package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTConst;
import com.tiktok.util.TTLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class TTAppEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventName;
    private Date timeStamp;
    private String jsonStr;
    private static AtomicLong counter = new AtomicLong(new Date().getTime() + 0L);
    private Long uniqueId;
    private static String TAG = TTAppEventsQueue.class.getCanonicalName();
    private static TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    TTAppEvent(TTConst.AppEventName eventName, String jsonStr) {
        this(eventName, new Date(), jsonStr);
    }

    TTAppEvent(TTConst.AppEventName eventName, Date timeStamp, String jsonStr) {
        this.eventName = eventName.toString();
        this.timeStamp = timeStamp;
        this.setJsonStr(jsonStr);
        this.uniqueId = TTAppEvent.counter.getAndIncrement();
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        try {
            new JSONObject(jsonStr);
            this.jsonStr = jsonStr;
        } catch (JSONException ex) {
            logger.info("Invalid jsonStr provided: " + jsonStr);
            this.jsonStr = "{}";
        }
    }

    public Long getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public String toString() {
        return "TTAppEvent{" +
                "eventName='" + eventName + '\'' +
                ", timeStamp=" + timeStamp +
                ", jsonObject='" + jsonStr + '\'' +
                ", uniqueId=" + uniqueId +
                '}';
    }
}
