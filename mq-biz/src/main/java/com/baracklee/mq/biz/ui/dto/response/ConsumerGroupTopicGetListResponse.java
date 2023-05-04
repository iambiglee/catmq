package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.ConsumerGroupTopicVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class ConsumerGroupTopicGetListResponse extends BaseUiResponse<List<ConsumerGroupTopicVo>> {

    public ConsumerGroupTopicGetListResponse(Long count, List<ConsumerGroupTopicVo> data) {
        super(count, data);
    }
}
