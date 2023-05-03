package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.NotifyMessageEntity;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class MessageNotifyResponse extends BaseUiResponse<List<NotifyMessageEntity>> {

    public MessageNotifyResponse(Long count, List<NotifyMessageEntity> data) {
        super(count, data);
    }

}
