package com.tiktok.appevents;

import java.util.ArrayList;
import java.util.List;

class TTAppEventsQueue {
    private static List<TTAppEvent> memory = new ArrayList<>();

    public static synchronized void addEvent(TTAppEvent event) {
        memory.add(event);
    }

    public static synchronized int size() {
        return memory.size();
    }

    public static synchronized List<TTAppEvent> exportAllEvents() {
        List<TTAppEvent> appEvents = memory;
        memory = new ArrayList<>();
        return appEvents;
    }

}
