package com.tiktok.util;

import android.util.Log;

import com.tiktok.TiktokBusinessSdk.LogLevel;

/** Logger util class */
public final class TTLogger {

    /** loglevel */
    public final LogLevel logLevel;
    /** log TAG */
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

    public void warn(String format, Object... extra) {
        if (shouldLog(LogLevel.WARN)) {
            Log.w(tag, String.format(format, extra));
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

    private boolean shouldLog(LogLevel level) {
        return logLevel.ordinal() >= level.ordinal();
    }
}
