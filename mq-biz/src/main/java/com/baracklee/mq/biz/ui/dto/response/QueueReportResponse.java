package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.QueueVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class QueueReportResponse extends BaseUiResponse<List<QueueVo>> {
    public QueueReportResponse(Long count,List<QueueVo> data){
        super(count,data);
    }
}
