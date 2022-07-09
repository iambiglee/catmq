package com.baracklee.mq.biz.common.util;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SoaConfig {

    @Resource
    private Environment env;

    private final String env_getSdkVersion_key = "mq.client.version";

    private final String env_getSdkVersion_defaultValue = PropUtil.getSdkVersion();


    public String getSdkVersion() {
        return env.getProperty(env_getSdkVersion_key, env_getSdkVersion_defaultValue);
    }

}
