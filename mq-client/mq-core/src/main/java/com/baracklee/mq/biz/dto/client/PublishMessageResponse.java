package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;

public class PublishMessageResponse extends BaseResponse {
    private long sleepTime;

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }
}
