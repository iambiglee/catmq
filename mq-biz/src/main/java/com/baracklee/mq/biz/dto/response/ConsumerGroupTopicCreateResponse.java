package com.baracklee.mq.biz.dto.response;

public class ConsumerGroupTopicCreateResponse extends BaseUiResponse<Void>{
    public ConsumerGroupTopicCreateResponse(){
        super();
    }
    public ConsumerGroupTopicCreateResponse(String code,String msg){
        super(code,msg);
    }

}
