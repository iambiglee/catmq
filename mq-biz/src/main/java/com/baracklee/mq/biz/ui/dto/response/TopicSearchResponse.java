package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.TopicEntity;

import java.util.List;

/**
 * @author Barack Lee
 */
public class TopicSearchResponse extends BaseUiResponse<List<TopicEntity>> {
    public TopicSearchResponse(Long count, List<TopicEntity> data) {
        super(count, data);
    }

    public TopicSearchResponse(){
        super();
    }
}
