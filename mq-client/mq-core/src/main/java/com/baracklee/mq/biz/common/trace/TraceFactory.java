package com.baracklee.mq.biz.common.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceFactory {
    private static Map<String, TraceMessage> traces = new ConcurrentHashMap<>();
    private static Object lockobj = new Object();
    private static volatile TraceCheck traceCheck;
    public static void setTraceCheck(TraceCheck traceCheck1){
        traceCheck=traceCheck1;
    }

    public static TraceMessage getInstance(String name){
        if(!traces.containsKey(name)){
            synchronized (lockobj){
                if(!traces.containsKey(name)){
                    traces.put(name,new TraceMessage(name));
                }
            }
        }
        return traces.get(name);
    }

    public static boolean isEnable(String name){
        if (traceCheck!=null){
            return traceCheck.isEnabled(name);
        }
        return true;
    }

    public interface TraceCheck{
        boolean isEnabled(String name);
    }
}
