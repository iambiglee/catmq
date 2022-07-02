package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;
import com.baracklee.mq.biz.dto.BaseResponse;

public class ConsumerRegisterResponse extends BaseResponse {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
