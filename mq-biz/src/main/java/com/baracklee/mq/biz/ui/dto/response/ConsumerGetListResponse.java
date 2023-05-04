package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.ConsumerVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class ConsumerGetListResponse extends BaseUiResponse<List<ConsumerVo>> {
    public ConsumerGetListResponse(Long count, List<ConsumerVo> data) {
        super(count, data);
    }

}
