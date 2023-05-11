package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.QueueOffsetVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class QueueOffsetGetListResponse extends BaseUiResponse<List<QueueOffsetVo>> {
    public QueueOffsetGetListResponse(Long count, List<QueueOffsetVo> data) {
        super(count, data);
    }
}
