package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;

/**
 * @author Barack Lee
 */
public class ConsumerGroupTopicInitResponse extends BaseUiResponse<ConsumerGroupTopicCreateRequest> {
    public ConsumerGroupTopicInitResponse(ConsumerGroupTopicCreateRequest data) {
        super(data);
    }
}
