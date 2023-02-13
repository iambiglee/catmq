package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;
import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.List;

public class GetTopicQueueIdsRequest extends BaseRequest {
    private List<String> topicNames;

    public List<String> getTopicNames() {
        return topicNames;
    }

    public void setTopicNames(List<String> topicNames) {
        this.topicNames = topicNames;
    }
}
