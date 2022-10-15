package com.baracklee.mq.biz.common;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.PropUtil;
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
    private final String env_enableTimer_key = "mq.enableTimer";
    private final String env_enableTimer_defaultValue = "1";
    public boolean enableTimer() {
        return "1".equals(env.getProperty(env_enableTimer_key, env_enableTimer_defaultValue));
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

    private volatile String _getMqLockHeartBeatTime = "";
    private volatile int getMqLockHeartBeatTime = 0;
    private final String env_getMqLockHeartBeatTime_key = "mq.lock.heartbeat.time";
    private final String env_getMqLockHeartBeatTime_defaultValue = "15";
    private final String env_getMqLockHeartBeatTime_des = "锁心跳发送时间间隔";

    // 锁心跳发送时间间隔
    public int getMqLockHeartBeatTime() {
        try {
            if (!_getMqLockHeartBeatTime
                    .equals(env.getProperty(env_getMqLockHeartBeatTime_key, env_getMqLockHeartBeatTime_defaultValue))) {
                _getMqLockHeartBeatTime = env.getProperty(env_getMqLockHeartBeatTime_key,
                        env_getMqLockHeartBeatTime_defaultValue);
                getMqLockHeartBeatTime = Integer.parseInt(
                        env.getProperty(env_getMqLockHeartBeatTime_key, env_getMqLockHeartBeatTime_defaultValue));
                if (getMqLockHeartBeatTime < 15) {
                    getMqLockHeartBeatTime = 15;
                }
                onChange();
            }
        } catch (Exception e) {
            getMqLockHeartBeatTime = 15;
            onChange();
        }
        return getMqLockHeartBeatTime;

    }

    private volatile String _isEnableRb = "";
    private volatile boolean isEnableRb = true;
    private final String env_isEnableRb_key = "mq.rb.enable";
    private final String env_isEnableRb_defaultValue = "true";
    private final String env_isEnableRb_des = "是否开启重平衡功能";

    // 是否开启重平衡功能
    public boolean isEnableRb() {
        try {
            if (!_isEnableRb.equals(env.getProperty(env_isEnableRb_key, env_isEnableRb_defaultValue))) {
                _isEnableRb = env.getProperty(env_isEnableRb_key, env_isEnableRb_defaultValue);
                isEnableRb = Boolean.parseBoolean(env.getProperty(env_isEnableRb_key, env_isEnableRb_defaultValue));
                onChange();
            }
            return isEnableRb;
        } catch (Exception e) {
            isEnableRb = true;
            onChange();
            return true;
        }
    }

}
