package com.baracklee.mq.biz.common.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceFactory {
    private static Map<String, TraceMessage> traces = new ConcurrentHashMap<>();
    private static Object lockobj = new Object();

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
}
