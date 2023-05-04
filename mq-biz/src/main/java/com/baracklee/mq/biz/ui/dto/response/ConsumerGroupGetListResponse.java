package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.ConsumerGroupVo;


import java.util.List;

/**
 * @author Barack Lee
 */
public class ConsumerGroupGetListResponse extends BaseUiResponse<List<ConsumerGroupVo>> {
    public ConsumerGroupGetListResponse(Long count, List<ConsumerGroupVo> data) {
        super(count, data);
    }
}
