package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;

/**
 * @author Barack Lee
 */
public class ConsumerGroupTopicGetByIdResponse extends BaseUiResponse<ConsumerGroupTopicEntity> {
    public ConsumerGroupTopicGetByIdResponse(ConsumerGroupTopicEntity data) {
        super(data);
    }
}
