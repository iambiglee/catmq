package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface TopicService extends BaseService<TopicEntity> {
    TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroupEntity);
}
