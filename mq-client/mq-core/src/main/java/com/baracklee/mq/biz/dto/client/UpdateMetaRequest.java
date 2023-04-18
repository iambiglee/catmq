package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;

public class UpdateMetaRequest extends BaseRequest {
    private List<String> consumerGroupName;

    public List<String> getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(List<String> consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }
}
