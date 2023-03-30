package com.baracklee.mq.biz.service;


import com.baracklee.mq.biz.dto.request.ConsumerGroupCreateRequest;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupCreateResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupEditResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface ConsumerGroupService extends BaseService<ConsumerGroupEntity> {
    List<ConsumerGroupEntity> getLastMetaConsumerGroup(long minMessageId, long maxMessageId);
    List<ConsumerGroupEntity> getLastRbConsumerGroup(long minMessageId, long maxMessageId);
    Map<String,ConsumerGroupEntity> getByNames(List<String> names);
    List<ConsumerGroupTopicEntity> getGroupTopic();
    List<ConsumerGroupEntity> getByOwnerNames(Map<String, Object> parameterMap);
    long countByOwnerNames(Map<String, Object> parameterMap);
    void rb(List<QueueOffsetEntity> queueOffsetEntities);
    void notifyRb(long consumerGroupId);
    void notifyRb(List<Long> consumerGroupIds);
    void notifyRbByNames(List<String> consumerGroupNames);
    void notifyMetaByNames(List<String> consumerGroupNames);
    void notifyMeta(long consumerGroupId);
    void notifyMeta(List<Long> consumerGroupIds);
    void notifyOffset(long consumerGroupId);
    void updateCache();
    void forceUpdateCache();
    Map<String,ConsumerGroupEntity> getCache();
    Map<String,ConsumerGroupEntity> getData();
    Map<Long,ConsumerGroupEntity> getIdCache();
    List<String> getSubEnvList();
    ConsumerGroupTopicEntity getTopic(String consumerGroupName,String topicName);
    ConsumerGroupCreateResponse createConsumerGroup(ConsumerGroupCreateRequest consumerGroupCreateRequest);
    ConsumerGroupEditResponse editConsumerGroup(ConsumerGroupEntity consumerGroupEntity);
    ConsumerGroupDeleteResponse deleteConsumerGroup(long consumerGroupId, boolean checkOnline);
    //按照新的名称重新生成一个新的组
    void copyAndNewConsumerGroup(ConsumerGroupEntity consumerGroupEntityOld,ConsumerGroupEntity consumerGroupEntityNew);
    BaseUiResponse deleteTopicNameFromConsumerGroup(ConsumerGroupTopicEntity consumerGroupTopicEntity);
    BaseUiResponse addTopicNameToConsumerGroup(ConsumerGroupTopicEntity consumerGroupTopicEntity);
    void deleteUnuseBroadConsumerGroup();
}
