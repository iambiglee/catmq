package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.TopicService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TopicServiceImpl extends AbstractBaseService<TopicEntity> implements TopicService {
    @Resource
    private UserInfoHolder userInfoHolder;

    @Override
    public TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroup) {
        TopicEntity failTopicEntity = new TopicEntity();
        failTopicEntity.setName(String.format("%s_%s_fail", consumerGroup.getName(), topicEntity.getName()));
        failTopicEntity.setOriginName(topicEntity.getOriginName());
        failTopicEntity.setDptName(topicEntity.getDptName());
        failTopicEntity.setOwnerIds(consumerGroup.getOwnerIds());
        failTopicEntity.setOwnerNames(consumerGroup.getOwnerNames());
        failTopicEntity.setEmails(consumerGroup.getAlarmEmails());
        failTopicEntity.setTels(topicEntity.getTels());
        failTopicEntity.setBusinessType(topicEntity.getBusinessType());
        failTopicEntity.setRemark(topicEntity.getRemark());
        failTopicEntity.setToken(topicEntity.getToken());
        failTopicEntity.setNormalFlag(topicEntity.getNormalFlag());
        failTopicEntity.setTopicType(2);
        failTopicEntity.setConsumerFlag(topicEntity.getConsumerFlag());
        failTopicEntity.setConsumerGroupNames(topicEntity.getConsumerGroupNames());
        failTopicEntity.setAppId(consumerGroup.getAppId());
        String userId = userInfoHolder.getUserId();
        failTopicEntity.setInsertBy(userId);
        if (getCache().containsKey(failTopicEntity.getName())) {
            return getCache().get(failTopicEntity.getName());
        } else {
            insert(failTopicEntity);
            distributeQueueWithLock(failTopicEntity, 2, 2);
            return getTopicByName(failTopicEntity.getName());
        }    }
}
