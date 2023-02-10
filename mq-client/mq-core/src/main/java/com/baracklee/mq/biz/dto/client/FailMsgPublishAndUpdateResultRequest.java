package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;

public class FailMsgPublishAndUpdateResultRequest extends BaseRequest {
    private List<Long> ids;

    private long queueId;

    private PublishMessageRequest failMsg;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public PublishMessageRequest getFailMsg() {
        return failMsg;
    }

    public void setFailMsg(PublishMessageRequest failMsg) {
        this.failMsg = failMsg;
    }
}
