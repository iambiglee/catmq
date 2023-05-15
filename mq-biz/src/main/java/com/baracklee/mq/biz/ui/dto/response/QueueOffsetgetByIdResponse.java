package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.QueueOffsetVo;

/**
 * @author Barack Lee
 */
public class QueueOffsetgetByIdResponse extends BaseUiResponse<QueueOffsetVo> {
    public QueueOffsetgetByIdResponse(QueueOffsetVo data) {
        super(data);
    }
}
