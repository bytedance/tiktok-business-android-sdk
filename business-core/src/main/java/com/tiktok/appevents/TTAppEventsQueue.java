package com.tiktok.appevents;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTUtil;

import java.util.ArrayList;
import java.util.List;

class TTAppEventsQueue {

    private static String TAG = TTAppEventsQueue.class.getCanonicalName();
    private static List<TTAppEvent> memory = new ArrayList<>();

    private static void notifyChange() {
        if (TiktokBusinessSdk.memoryListener != null) {
            TiktokBusinessSdk.memoryListener.onMemoryChange(memory.size());
        }

        if (TiktokBusinessSdk.nextTimeFlushListener != null) {
            int left = TTAppEventLogger.THRESHOLD - size();
            TiktokBusinessSdk.nextTimeFlushListener.thresholdLeft(TTAppEventLogger.THRESHOLD, left > 0 ? left : 0);
        }
    }

    public static synchronized void addEvent(TTAppEvent event) {
        TTUtil.checkThread(TAG);
        memory.add(event);
        notifyChange();
    }

    public static synchronized int size() {
        return memory.size();
    }

    public static synchronized void clearAll(){
        TTUtil.checkThread(TAG);
        memory = new ArrayList<>();
        notifyChange();
    }

    public static synchronized List<TTAppEvent> exportAllEvents() {
        List<TTAppEvent> appEvents = memory;
        memory = new ArrayList<>();
        notifyChange();
        return appEvents;
    }


}
