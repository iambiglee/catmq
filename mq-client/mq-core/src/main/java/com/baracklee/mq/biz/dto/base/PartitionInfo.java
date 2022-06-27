package com.baracklee.mq.biz.dto.base;

public class PartitionInfo {

    private long queueId;

    private int strictMode =1;

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public int getStrictMode() {
        return strictMode;
    }

    public void setStrictMode(int strictMode) {
        this.strictMode = strictMode;
    }
}
