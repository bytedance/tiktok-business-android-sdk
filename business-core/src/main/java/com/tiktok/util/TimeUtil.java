package com.tiktok.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    /**
     * 传入Data类型日期，返回字符串类型时间（ISO8601标准时间）
     * @param date
     * @return
     */
    public static String getISO8601Timestamp(Date date){
//        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
//        df.setTimeZone(tz);
        return df.format(date);
    }
}
