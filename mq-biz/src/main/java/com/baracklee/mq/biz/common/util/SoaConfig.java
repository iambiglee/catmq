package com.baracklee.mq.biz.common.util;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class SoaConfig {

    @Resource
    private Environment env;

    private final String env_getSdkVersion_key = "mq.client.version";
    private Map<Runnable, Boolean> changed = new ConcurrentHashMap<>();
    private final String env_getSdkVersion_defaultValue = PropUtil.getSdkVersion();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("SoaConfig-scan", true),
            new ThreadPoolExecutor.DiscardPolicy());


    public String getSdkVersion() {
        return env.getProperty(env_getSdkVersion_key, env_getSdkVersion_defaultValue);
    }

    private volatile String mqMetaRebuildMaxInterval="";
    private final String env_getMqMetaRebuildMaxInterval_key = "mq.meta.rebuild.max.interval";
    private final String env_getMqMetaRebuildMaxInterval_defaultValue = "120000";
    private volatile int getMqMetaRebuildMaxInterval = 120000;
    public int getMetaMqRebuildMaxInterval() {
        if(!mqMetaRebuildMaxInterval.equals(env.getProperty(env_getMqMetaRebuildMaxInterval_key,env_getMqMetaRebuildMaxInterval_defaultValue))){
            mqMetaRebuildMaxInterval=env.getProperty(env_getMqMetaRebuildMaxInterval_key,env_getMqMetaRebuildMaxInterval_defaultValue);
            getMqMetaRebuildMaxInterval=Integer.parseInt(env.getProperty(env_getMqMetaRebuildMaxInterval_key,env_getMqMetaRebuildMaxInterval_defaultValue));
            if(getMqMetaRebuildMaxInterval<5000){
                getMqMetaRebuildMaxInterval=5000;
            }
            onChange();
        }
        return getMqMetaRebuildMaxInterval;
    }

    private void onChange() {
        executor.execute(()->{
            for (Runnable runnable : changed.keySet()) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
