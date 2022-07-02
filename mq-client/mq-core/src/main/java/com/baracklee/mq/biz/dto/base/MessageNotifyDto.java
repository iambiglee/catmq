package com.baracklee.mq.biz.dto.base;

public class MessageNotifyDto {
    private long queueId;
    private String consumeGroupName;

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public String getConsumeGroupName() {
        return consumeGroupName;
    }

    public void setConsumeGroupName(String consumeGroupName) {
        this.consumeGroupName = consumeGroupName;
    }
}
