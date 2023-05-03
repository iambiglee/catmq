package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.MessageVo;

import java.util.List;

/**
 * @Author： Barack Lee
 * @Package： com.baracklee.mq.biz.ui.dto.response
 * @Project： myMessageQueue
 * @name： MessageGetListResponse
 * @Filename： MessageGetListResponse
 */
public class MessageGetListResponse extends BaseUiResponse<List<MessageVo>>{

    public MessageGetListResponse(Long count, List<MessageVo> data){
        super(count,data);
    }
}
