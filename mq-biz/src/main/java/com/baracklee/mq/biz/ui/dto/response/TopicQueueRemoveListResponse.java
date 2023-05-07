package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.QueueRemoveInfoVo;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class TopicQueueRemoveListResponse extends BaseUiResponse<List<QueueRemoveInfoVo>> {


    public TopicQueueRemoveListResponse(Long count, List<QueueRemoveInfoVo> data) {
        super(count, data);
    }



}
