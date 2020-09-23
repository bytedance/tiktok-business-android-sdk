package com.tiktok.appevents;

import com.tiktok.model.TTAppEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//@Data
//@NoArgsConstructor
class TTAppEventPersist implements Serializable {

    public static final long serialVersionUID = 1L;

    private List<TTAppEvent> appEvents = new ArrayList<>();

    public void addEvents(List<TTAppEvent> appEventList) {
        if(appEventList == null || appEventList.size() == 0) {
            return;
        }

        appEvents.addAll(appEventList);
    }

    public List<TTAppEvent> getAppEvents() {
        return appEvents;
    }

    public void setAppEvents(List<TTAppEvent> appEvents) {
        this.appEvents = appEvents;
    }

    public boolean isEmpty(){
        return appEvents.isEmpty();
    }
}
