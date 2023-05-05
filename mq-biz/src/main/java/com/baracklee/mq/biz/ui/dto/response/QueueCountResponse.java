package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.Map;

/**
 * @author Barack Lee
 */
public class QueueCountResponse extends BaseUiResponse<Map<String,Long>> {
    public QueueCountResponse(Map<String, Long> data) {
        super(data);
    }

}
