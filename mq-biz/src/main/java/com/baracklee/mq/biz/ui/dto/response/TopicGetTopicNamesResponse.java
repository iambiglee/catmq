package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

/**
 * @author Barack Lee
 */
public class TopicGetTopicNamesResponse extends BaseUiResponse<List<String>> {
    public TopicGetTopicNamesResponse(Long count, List<String> data) {
        super(count, data);
    }

}
