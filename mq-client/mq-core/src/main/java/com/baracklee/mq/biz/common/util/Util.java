package com.baracklee.mq.biz.common.util;

public class Util {
    public static boolean isEmpty(String str){
        return (str == null || "".equals(str)||str.trim().length()==0);
    }
}
