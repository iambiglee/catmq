package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface TopicService extends BaseService<TopicEntity> {
    public static String NEED_DELETED_TOPIC_NANE="MQ_NEED_DELETE_1111111";
    Map<String, TopicEntity> getCache();

    public void distributeQueue(TopicEntity normalTopicEntity, QueueEntity queueEntity);

    public void distributeQueue(TopicEntity topicEntity, List<QueueEntity> queueEntityList);

    TopicEntity getTopicByName(String topicName);

    List<TopicEntity> getListWithUserName(Map<String, Object> conditionMap, long page, long pageSize);

    void updateCache();

    long countWithUserName(Map<String, Object> conditionMap);

    void updateFailTopic(ConsumerGroupEntity consumerGroupEntity);

    void deleteFailTopic(List<String> failTopicNames, long consumerGroupId);

    TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroup);

    void distributeQueueWithLock(TopicEntity topicEntity, int queueNum, int nodeType);

    long getMsgCount(String topicName, String start, String end);

}
