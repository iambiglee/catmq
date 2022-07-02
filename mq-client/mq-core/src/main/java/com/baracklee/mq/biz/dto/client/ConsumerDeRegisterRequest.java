package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;

public class ConsumerDeRegisterRequest extends BaseRequest {
    private long id;
    private List<String> consumerGroupNames;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getConsumerGroupNames() {
        return consumerGroupNames;
    }

    public void setConsumerGroupNames(List<String> consumerGroupNames) {
        this.consumerGroupNames = consumerGroupNames;
    }
}
