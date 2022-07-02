package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.Map;

public class ConsumerGroupRegisterResponse extends BaseResponse {
    private Map<String,String> broadcastConsumerGroupName;

    private Map<String,String> consumerGroupNameNew;

    public Map<String, String> getBroadcastConsumerGroupName() {
        return broadcastConsumerGroupName;
    }

    public void setBroadcastConsumerGroupName(Map<String, String> broadcastConsumerGroupName) {
        this.broadcastConsumerGroupName = broadcastConsumerGroupName;
    }

    public Map<String, String> getConsumerGroupNameNew() {
        return consumerGroupNameNew;
    }

    public void setConsumerGroupNameNew(Map<String, String> consumerGroupNameNew) {
        this.consumerGroupNameNew = consumerGroupNameNew;
    }
}
