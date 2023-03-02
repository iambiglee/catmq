package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.response.ConsumerGroupTopicCreateResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ConsumerGroupTopicService extends BaseService<ConsumerGroupTopicEntity> {
    Map<Long, Map<String, ConsumerGroupTopicEntity>> getCache();

    ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest request2, Map<String, ConsumerGroupEntity> consumerGroupMap);

    Map<String, ConsumerGroupTopicEntity> getGroupTopic();

    List<String> getFailTopicNames(long id);

    void deleteByConsumerGroupId(long id);
}
