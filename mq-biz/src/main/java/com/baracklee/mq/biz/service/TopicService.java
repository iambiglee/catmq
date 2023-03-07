package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface TopicService extends BaseService<TopicEntity> {
    public static String NEED_DELETED_TOPIC_NANE="MQ_NEED_DELETE_1111111";

    TopicEntity getTopicByName(String topicName);

    TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroupEntity);

    void deleteFailTopic(List<String> failTopicNames, long id);

    Map<String, TopicEntity> getCache();
}
