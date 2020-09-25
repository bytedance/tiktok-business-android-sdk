package com.tiktok.appevents;

import java.io.Serializable;
import java.util.Date;

class TTAppEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventName;
    private Date timeStamp;
    private String jsonObject;

    public TTAppEvent(String eventName, String jsonObject) {
        this(eventName, new Date(), jsonObject);
    }

    public TTAppEvent(String eventName, Date timeStamp, String jsonObject) {
        this.eventName = eventName;
        this.timeStamp = timeStamp;
        this.jsonObject = jsonObject;
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
}
