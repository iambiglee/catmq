package com.baracklee.mq.biz.dto;

import com.baracklee.mq.biz.common.util.IPUtil;

public class BaseRequest {
    private String lan;

    private String sdkVersion;

    private String clientIp= IPUtil.getLocalIP();
}
