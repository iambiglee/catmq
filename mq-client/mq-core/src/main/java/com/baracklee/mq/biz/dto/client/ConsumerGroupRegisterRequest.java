package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;
import java.util.Map;

public class ConsumerGroupRegisterRequest extends BaseRequest {
    private long consumerId;
    private String consumerName;
    private String subEnv="default";
    private Map<String, List<String>> consumerGroupNames;

    public long getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(long consumerId) {
        this.consumerId = consumerId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public String getSubEnv() {
        return subEnv;
    }

    public void setSubEnv(String subEnv) {
        this.subEnv = subEnv;
    }

    public Map<String, List<String>> getConsumerGroupNames() {
        return consumerGroupNames;
    }

    public void setConsumerGroupNames(Map<String, List<String>> consumerGroupNames) {
        this.consumerGroupNames = consumerGroupNames;
    }
}
