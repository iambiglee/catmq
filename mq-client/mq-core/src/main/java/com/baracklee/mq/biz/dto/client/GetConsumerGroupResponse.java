package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;
import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;

import java.util.Map;
import java.util.Set;

public class GetConsumerGroupResponse extends BaseResponse {
    private long sleepTime;

    private Map<String, ConsumerGroupOneDto> consumerGroups;

    private Map<String, Set<String>> consumerGroupoSubEnvMap;

    private int consumerDeleted;

    private int brokerMetaMode;

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Map<String, ConsumerGroupOneDto> getConsumerGroups() {
        return consumerGroups;
    }

    public void setConsumerGroups(Map<String, ConsumerGroupOneDto> consumerGroups) {
        this.consumerGroups = consumerGroups;
    }

    public Map<String, Set<String>> getConsumerGroupoSubEnvMap() {
        return consumerGroupoSubEnvMap;
    }

    public void setConsumerGroupoSubEnvMap(Map<String, Set<String>> consumerGroupoSubEnvMap) {
        this.consumerGroupoSubEnvMap = consumerGroupoSubEnvMap;
    }

    public int getConsumerDeleted() {
        return consumerDeleted;
    }

    public void setConsumerDeleted(int consumerDeleted) {
        this.consumerDeleted = consumerDeleted;
    }

    public int getBrokerMetaMode() {
        return brokerMetaMode;
    }

    public void setBrokerMetaMode(int brokerMetaMode) {
        this.brokerMetaMode = brokerMetaMode;
    }
}
