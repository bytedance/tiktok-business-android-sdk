package com.tiktok.util;

import android.util.Log;

import com.tiktok.TiktokBusinessSdk.LogLevel;

import static com.tiktok.util.TTConst.DEFAULT_TAG;

public final class TTLogger {

    public final LogLevel logLevel;
    private final String tag;

    public TTLogger(String tag, LogLevel logLevel) {
        this.tag = tag;
        this.logLevel = logLevel;
    }

    public void verbose(String format, Object... extra) {
        if (shouldLog(LogLevel.VERBOSE)) {
            Log.v(tag, String.format(format, extra));
        }
    }

    public void info(String format, Object... extra) {
        if (shouldLog(LogLevel.INFO)) {
            Log.i(tag, String.format(format, extra));
        }
    }

    public void debug(String format, Object... extra) {
        if (shouldLog(LogLevel.DEBUG)) {
            Log.d(tag, String.format(format, extra));
        }
    }

    public void error(Throwable error, String format, Object... extra) {
        if (shouldLog(LogLevel.INFO)) {
            Log.e(tag, String.format(format, extra), error);
        }
    }

    public TTLogger subLog(String tag) {
        return new TTLogger(DEFAULT_TAG + "-" + tag, logLevel);
    }

    public static TTLogger with(LogLevel level) {
        return new TTLogger(DEFAULT_TAG, level);
    }

    private boolean shouldLog(LogLevel level) {
        return logLevel.ordinal() >= level.ordinal();
    }
}
