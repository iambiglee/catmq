package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.List;
import java.util.Map;

public class GetTopicQueueIdsResponse extends BaseResponse {
    private Map<String, List<Long>> topicQueues;

    public Map<String, List<Long>> getTopicQueues() {
        return topicQueues;
    }

    public void setTopicQueues(Map<String, List<Long>> topicQueues) {
        this.topicQueues = topicQueues;
    }
}
