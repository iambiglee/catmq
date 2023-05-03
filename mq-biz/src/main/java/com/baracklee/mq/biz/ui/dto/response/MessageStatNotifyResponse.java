package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class MessageStatNotifyResponse extends BaseUiResponse<List<NotifyMessageStatEntity>> {

    public MessageStatNotifyResponse(Long count, List<NotifyMessageStatEntity> data) {
        super(count, data);
    }

}
