package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.List;

public class GetMetaResponse extends BaseResponse {
    private List<String> brokerIp;
    private int brokerMetaMode;

    public List<String> getBrokerIp() {
        return brokerIp;
    }

    public void setBrokerIp(List<String> brokerIp) {
        this.brokerIp = brokerIp;
    }

    public int getBrokerMetaMode() {
        return brokerMetaMode;
    }

    public void setBrokerMetaMode(int brokerMetaMode) {
        this.brokerMetaMode = brokerMetaMode;
    }
}
