package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.TopicEntity;

/**
 * @Author： Barack Lee
 */
public class TopicGetByIdResponse extends BaseUiResponse<TopicEntity> {
    public TopicGetByIdResponse(TopicEntity data) {
        super(data);
    }

}
