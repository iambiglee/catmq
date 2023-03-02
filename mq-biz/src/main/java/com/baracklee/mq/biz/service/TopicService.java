package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface TopicService extends BaseService<TopicEntity> {
    TopicEntity getTopicByName(String topicName);

    TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroupEntity);

    void deleteFailTopic(List<String> failTopicNames, long id);

    Map<String, TopicEntity> getCache();
}
