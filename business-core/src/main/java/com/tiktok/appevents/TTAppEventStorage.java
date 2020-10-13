package com.tiktok.appevents;

import android.content.Context;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

class TTAppEventStorage {
    private static final String TAG = TTAppEventStorage.class.getCanonicalName();

    private static final TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private static final String EVENT_STORAGE_FILE = "events_cache";

    private static final int MAX_PERSIST_EVENTS_NUM = 10000;

    /**
     * write events into file
     * @param failedEvents if flush failed, failedEvents is not null
     */
    public synchronized static void persist(List<TTAppEvent> failedEvents) {

        List<TTAppEvent> appEventList = TTAppEventsQueue.exportAllEvents();

        TTAppEventPersist appEventPersist = readFromDisk();

        if(appEventList.isEmpty() && appEventPersist.isEmpty() &&
                (failedEvents == null || failedEvents.isEmpty())){
            return;
        }

        if(failedEvents != null) {
            appEventPersist.addEvents(failedEvents);
        }

        appEventPersist.addEvents(appEventList);

        //If end up persisting more than 10,000 events, persist the latest 10,000 events by timestamp
        slimEvents(appEventPersist);

        saveToDisk(appEventPersist);
    }

    /**
     * events slim
     * @param ttAppEventPersist
     */
    private static void slimEvents(TTAppEventPersist ttAppEventPersist){
        if(ttAppEventPersist == null || ttAppEventPersist.isEmpty()) {
            return;
        }

        List<TTAppEvent> appEvents = ttAppEventPersist.getAppEvents();

        int size = appEvents.size();

        if(size > MAX_PERSIST_EVENTS_NUM) {
            ttAppEventPersist.setAppEvents(appEvents.subList(size-MAX_PERSIST_EVENTS_NUM, size));
        }
    }

    private static boolean saveToDisk(TTAppEventPersist appEventPersist) {
        if(appEventPersist.isEmpty()) {
            return false;
        }

        Context context = TiktokBusinessSdk.getApplicationContext();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(context.openFileOutput(EVENT_STORAGE_FILE, Context.MODE_PRIVATE)));
            oos.writeObject(appEventPersist);
            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
    }

    public synchronized static TTAppEventPersist readFromDisk() {
        Context context = TiktokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        if (!f.exists()) {
            return new TTAppEventPersist();
        }

        TTAppEventPersist appEventPersist = new TTAppEventPersist();

        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new BufferedInputStream(context.openFileInput(EVENT_STORAGE_FILE)));

            appEventPersist = (TTAppEventPersist) ois.readObject();

            f.delete();
        } catch (ClassNotFoundException e) {
            if (f.exists()) {
                f.delete();
            }
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return appEventPersist;
    }

}
