package com.baracklee.mq.biz.common.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class Util {

    private final static String DEFAULT_FORMATE = "yyyy-MM-dd HH:mm:ss";
    public final static String  SSS_FORMATE= "yyyy-MM-dd HH:mm:ss:SSS";

    public static boolean isEmpty(String str){
        return (str == null || "".equals(str)||str.trim().length()==0);
    }

    public static String formateDate(Date date, String formate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formate);
        return simpleDateFormat.format(date);
    }

    public static String formateDate(Date date) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_FORMATE);
            return simpleDateFormat.format(date);
        } catch (Throwable e) {
            return null;
        }
    }

    public static void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ignored) {

        }

    }

    public static Integer getProcessId() {
        RuntimeMXBean runtime= ManagementFactory.getRuntimeMXBean();
        String name=runtime.getName();
        return Integer.parseInt(name.substring(0,name.indexOf("@")));
    }
}
