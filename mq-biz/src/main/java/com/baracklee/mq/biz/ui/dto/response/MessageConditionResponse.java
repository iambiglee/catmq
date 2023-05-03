package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.Map;

/**
 * @Author： Barack Lee
 */
public class MessageConditionResponse extends BaseUiResponse<Map<String,Object>> {
    public MessageConditionResponse(Map<String,Object> data){
        super(data);
    }
}
