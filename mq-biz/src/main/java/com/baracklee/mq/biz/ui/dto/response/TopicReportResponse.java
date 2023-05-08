package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.TopicVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class TopicReportResponse extends BaseUiResponse<List<TopicVo>> {
    public TopicReportResponse(Long count, List<TopicVo> data) {
        super(count, data);
    }
}
