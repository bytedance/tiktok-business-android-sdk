package com.tiktok.appevents;

import java.util.concurrent.ThreadFactory;

public class TTThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        final Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new TTUncaughtExceptionHandler());
        return t;
    }
}
