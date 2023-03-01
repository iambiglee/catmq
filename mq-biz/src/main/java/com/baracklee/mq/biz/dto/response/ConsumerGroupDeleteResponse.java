package com.baracklee.mq.biz.dto.response;

public class ConsumerGroupDeleteResponse extends BaseUiResponse<Void>{
    public ConsumerGroupDeleteResponse() {
        super();
    }

    public ConsumerGroupDeleteResponse(String code, String msg) {
        super(code, msg);
    }
}
