package com.tiktok.appevents;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class TTAppEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventName;
    private Date timeStamp;
    private String jsonObject;
    private static AtomicLong counter = new AtomicLong(new Date().getTime() + 0L);
    private Long uniqueId;

    TTAppEvent(String eventName, String jsonObject) {
        this(eventName, new Date(), jsonObject);
    }

    TTAppEvent(String eventName, Date timeStamp, String jsonObject) {
        this.eventName = eventName;
        this.timeStamp = timeStamp;
        this.jsonObject = jsonObject;
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

    public String getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(String jsonObject) {
        this.jsonObject = jsonObject;
    }

    public Long getUniqueId() {
        return this.uniqueId;
    }
}
