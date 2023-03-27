package com.baracklee.mq.biz.dto.client;

public class MsgNotifyDto {
    private long queueId;
    private String consumerGroupname;

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public String getConsumerGroupname() {
        return consumerGroupname;
    }

    public void setConsumerGroupname(String consumerGroupname) {
        this.consumerGroupname = consumerGroupname;
    }
}
