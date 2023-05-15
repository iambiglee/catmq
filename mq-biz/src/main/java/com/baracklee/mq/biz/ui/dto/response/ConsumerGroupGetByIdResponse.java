package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;

/**
 * @author Barack Lee
 */
public class ConsumerGroupGetByIdResponse extends BaseUiResponse<ConsumerGroupEntity> {
    public ConsumerGroupGetByIdResponse(ConsumerGroupEntity data) {
        super(data);
    }
}
