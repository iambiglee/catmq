package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.Message01Entity;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class MessageGetByTopicResponse extends BaseUiResponse<List<Message01Entity>> {

    public MessageGetByTopicResponse(List<Message01Entity> data){
        super(data);
    }

    public MessageGetByTopicResponse(String code, String msg){super(code,msg);}
}
