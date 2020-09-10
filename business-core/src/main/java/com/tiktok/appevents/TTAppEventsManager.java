package com.tiktok.appevents;

import android.util.Log;

import com.tiktok.enums.FlushReason;
import com.tiktok.model.TTAppEvent;
import com.tiktok.model.TTRequest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TTAppEventsManager {

    private static String TAG = TTAppEventsManager.class.getCanonicalName();
    private static final int TIME_BUFFER = 15;
    private static final int THRESHOLD = 5;

    private static int flushId = 0;

    private static final ScheduledExecutorService eventLoop =
            Executors.newSingleThreadScheduledExecutor();

    private static ScheduledFuture<?> flushFuture = null;

    private static Runnable batchFlush = new Runnable() {
        @Override
        public void run() {
            flushFuture = null;
            flush(FlushReason.TIMER);
        }
    };

    public static void logEvent(final TTAppEvent event) {
        eventLoop.execute(new Runnable() {
            @Override
            public void run() {
                TTAppEventsQueue.addEvent(event);

                if (TTAppEventsQueue.size() > THRESHOLD) {
                    flush(FlushReason.THRESHOLD);
                } else if (flushFuture == null) {
                    flushFuture = eventLoop.schedule(batchFlush, TIME_BUFFER, TimeUnit.SECONDS);
                }
            }
        });

    }

    private static void flush(FlushReason reason) {
        Log.v(TAG, "=================================================================");
        Log.v(TAG, String.format("Start flush, version %d reason is %s", flushId, reason.name()));
        // currently, print out everything
        TTAppEventPersist appEventPersist = TTAppEventStorage.readFromDisk();

        appEventPersist.addEvents(TTAppEventsQueue.exportAllEvents());

        for (TTAppEvent event : appEventPersist.getAppEvents()) {
            Log.v(TAG, event.toString());
        }

        TTRequest.test();

        Log.v(TAG, String.format("END flush, version %d reason is %s", flushId, reason.name()));
        Log.v(TAG, "=================================================================");

        flushId++;
    }
}
