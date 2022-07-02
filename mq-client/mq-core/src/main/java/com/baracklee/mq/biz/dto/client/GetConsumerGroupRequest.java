package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.Map;

public class GetConsumerGroupRequest extends BaseResponse {
    private long consumerId;
    private Map<String, Long> consumerGroupVersion;

    public long getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(long consumerId) {
        this.consumerId = consumerId;
    }

    public Map<String, Long> getConsumerGroupVersion() {
        return consumerGroupVersion;
    }

    public void setConsumerGroupVersion(Map<String, Long> consumerGroupVersion) {
        this.consumerGroupVersion = consumerGroupVersion;
    }
}
