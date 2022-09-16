package com.baracklee.mq.client;

import com.baracklee.mq.biz.MqEnv;

import java.util.List;
import java.util.Set;

public interface MqEnvironment {
    boolean isPro();
    String getAppId();
    MqEnv getEnv();
    String getSubEnv();
    String getTargetSubEnv();
    void setTargetSubEnv(String targetSubEnv1);
    void clear();
    Set<String> getAppSubEnvs();
    void setAppSubEnvs(List<String> appSubEnvs);
}
