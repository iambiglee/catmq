package com.baracklee.mq.biz.dto.response;

public class ConsumerGroupCreateResponse extends BaseUiResponse<Void> {
    public ConsumerGroupCreateResponse() {
        super();
    }

    public ConsumerGroupCreateResponse(String code, String msg) {
        super(code,msg);
    }
}
