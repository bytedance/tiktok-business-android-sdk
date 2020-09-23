package com.tiktok.enums;

public enum FlushReason {
    THRESHOLD, // when reaching the threshold of the event queue
    TIMER, // triggered every 15 seconds
    START_UP // when app is started, flush all the accumulated eents
}
