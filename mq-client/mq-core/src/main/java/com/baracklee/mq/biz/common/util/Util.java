package com.baracklee.mq.biz.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

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
}
