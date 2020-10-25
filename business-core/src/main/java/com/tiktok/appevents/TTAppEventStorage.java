package com.tiktok.appevents;

import android.content.Context;

import com.tiktok.TiktokBusinessSdk;
import com.tiktok.util.TTLogger;
import com.tiktok.util.TTUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

class TTAppEventStorage {
    private static final String TAG = TTAppEventStorage.class.getCanonicalName();

    private static final TTLogger logger = new TTLogger(TAG, TiktokBusinessSdk.getLogLevel());

    private static final String EVENT_STORAGE_FILE = "events_cache";

    private static final int MAX_PERSIST_EVENTS_NUM = 500;

    /**
     * write events into file
     *
     * @param failedEvents if flush failed, failedEvents is not null
     */
    public synchronized static void persist(List<TTAppEvent> failedEvents) {
        TTUtil.checkThread(TAG);

        logger.verbose("Tried to persist to disk");
        if (!TiktokBusinessSdk.isSystemActivated()) {
            logger.verbose("Quit persisting to disk because global switch is turned off");
            return;
        }

        List<TTAppEvent> eventsFromMemory = TTAppEventsQueue.exportAllEvents();

        TTAppEventPersist eventsFromDisk = readFromDisk();

        if (eventsFromMemory.isEmpty() && eventsFromDisk.isEmpty() &&
                (failedEvents == null || failedEvents.isEmpty())) {
            return;
        }

        TTAppEventPersist toBeSaved = new TTAppEventPersist();
        // maintain events ordering, the events in the network should be earlier than the
        // events on the disk, finally come the events in the memory
        if (failedEvents != null) {
            toBeSaved.addEvents(failedEvents);
        }
        toBeSaved.addEvents(eventsFromDisk.getAppEvents());
        toBeSaved.addEvents(eventsFromMemory);

        //If end up persisting more than 10,000 events, persist the latest 10,000 events by timestamp
        discardOldEvents(toBeSaved, MAX_PERSIST_EVENTS_NUM);
        saveToDisk(toBeSaved);
    }

    private static int totalDumped = 0;

    /**
     * discard old events
     * In order not to overwhelm users' disk, only maxPersistNum is allowed to be persisted to disk
     *
     * @param ttAppEventPersist
     */
    private static void discardOldEvents(TTAppEventPersist ttAppEventPersist, int maxPersistNum) {
        if (ttAppEventPersist == null || ttAppEventPersist.isEmpty()) {
            return;
        }

        List<TTAppEvent> appEvents = ttAppEventPersist.getAppEvents();

        int size = appEvents.size();

        if (size > maxPersistNum) {
            logger.verbose("Way too many events(%d), slim it!", size);
            totalDumped += size - maxPersistNum;
            TiktokBusinessSdk.diskListener.onDumped(totalDumped);
            ttAppEventPersist.setAppEvents(new ArrayList<>(appEvents.subList(size - maxPersistNum, size)));
        }
    }

    private static boolean saveToDisk(TTAppEventPersist appEventPersist) {
        if (appEventPersist.isEmpty()) {
            return false;
        }

        Context context = TiktokBusinessSdk.getApplicationContext();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(context.openFileOutput(EVENT_STORAGE_FILE, Context.MODE_PRIVATE)))) {
            oos.writeObject(appEventPersist);
            logger.verbose("Saving %d events to disk", appEventPersist.getAppEvents().size());
            if (TiktokBusinessSdk.diskListener != null) {
                TiktokBusinessSdk.diskListener.onDiskChange(appEventPersist.getAppEvents().size(), false);
            }
            return true;
        } catch (Exception e) {
            TTCrashHandler.handleCrash(TAG, e);
            return false;
        }
    }

    private static void deleteFile(File f) {
        if (f.exists()) {
            f.delete();
        }
    }

    synchronized static TTAppEventPersist readFromDisk() {
        TTUtil.checkThread(TAG);

        Context context = TiktokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        if (!f.exists()) {
            return new TTAppEventPersist();
        }

        TTAppEventPersist appEventPersist = new TTAppEventPersist();

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(context.openFileInput(EVENT_STORAGE_FILE)))) {
            appEventPersist = (TTAppEventPersist) ois.readObject();
            logger.verbose("disk read data: %s", appEventPersist);
            deleteFile(f);
            if (TiktokBusinessSdk.diskListener != null) {
                TiktokBusinessSdk.diskListener.onDiskChange(0, true);
            }
        } catch (ClassNotFoundException e) {
            deleteFile(f);
            TTCrashHandler.handleCrash(TAG, e);
        } catch (Exception e) {
            deleteFile(f);
            TTCrashHandler.handleCrash(TAG, e);
        }
        return appEventPersist;
    }

    public synchronized static void clearAll() {
        TTUtil.checkThread(TAG);

        Context context = TiktokBusinessSdk.getApplicationContext();
        File f = new File(context.getFilesDir(), EVENT_STORAGE_FILE);
        deleteFile(f);
        if (TiktokBusinessSdk.diskListener != null) {
            TiktokBusinessSdk.diskListener.onDiskChange(0, true);
        }
    }
}
