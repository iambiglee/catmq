package com.baracklee.mq.biz.ui.dto.request;

import com.baracklee.mq.biz.dto.request.BaseUiRequst;

/**
 * @author Barack Lee
 */
public class ConsumerGetListRequest extends BaseUiRequst {
    private Long id;

    private String ip;

    private String consumerGroupNames;

    private String sdkVersion;

    private String compareType;

    private String heartBeat;

    public String getHeartBeat() {
        return heartBeat;
    }

    public void setHeartBeat(String heartBeat) {
        this.heartBeat = heartBeat;
    }

    public String getCompareType() {
        return compareType;
    }

    public void setCompareType(String compareType) {
        this.compareType = compareType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getConsumerGroupNames() {
        return consumerGroupNames;
    }

    public void setConsumerGroupNames(String consumerGroupNames) {
        this.consumerGroupNames = consumerGroupNames;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }
}
