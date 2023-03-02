package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dal.meta.TopicRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.TopicService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class TopicServiceImpl extends AbstractBaseService<TopicEntity> implements TopicService {

    private Logger log = LoggerFactory.getLogger(TopicServiceImpl.class);

    @Resource
    private UserInfoHolder userInfoHolder;
    @Resource
    private TopicRepository topicRepository;

    @Resource
    QueueService queueService;


    @Override
    public TopicEntity getTopicByName(String topicName) {
        return topicRepository.getTopicByName(topicName);
    }
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

    @Override
    public void deleteFailTopic(List<String> failTopicNames, long id) {
        if (CollectionUtils.isEmpty(failTopicNames)) return;
        for (String failTopicName : failTopicNames) {
            TopicEntity topicByName = getTopicByName(failTopicName);
            if(topicByName!=null){
                List<QueueEntity> queueEntities = queueService.getQueuesByTopicId(topicByName.getId());
                queueService.deleteMessage(queueEntities,id);
                delete(topicByName.getId());
                log.warn("consumer_group_{},删除失败topic{}",id, JsonUtil.toJson(topicByName));
            }
        }
    }

    @Override
    public Map<String, TopicEntity> getCache() {
        List<TopicEntity> topicEntities = topicRepository.getAll();
        MqReadMap<String, TopicEntity> topicCacheMap = new MqReadMap<String, TopicEntity>(data.size());
        topicCacheMap.setOnlyRead();
        for (TopicEntity topicEntity : topicEntities) {
            topicCacheMap.put(topicEntity.getName(),topicEntity);
        }
        return topicCacheMap;
    }
}
