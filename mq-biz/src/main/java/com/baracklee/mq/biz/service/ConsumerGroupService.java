package com.baracklee.mq.biz.service;


import com.baracklee.mq.biz.dto.response.ConsumerGroupDeleteResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface ConsumerGroupService extends BaseService<ConsumerGroupEntity> {
    Map<String, ConsumerGroupEntity> getCache();
    void copyAndNewConsumerGroup(ConsumerGroupEntity consumerGroupEntityOld,ConsumerGroupEntity consumerGroupEntityNew);

    void updateCache();
    void forceUpdateCache();
    Map<String,ConsumerGroupEntity> getByNames(List<String> names);

    void notifyRb(List<Long> ids);

    void notifyRb(Long ids);

    List<ConsumerGroupEntity> getLastRbConsumerGroup(long lastNotifyMessageId, long currentMaxId);

    void rb(List<QueueOffsetEntity> queueOffsets);

    void addTopicNameToConsumerGroup(ConsumerGroupTopicEntity consumerGroupTopicEntity);

    void notifyMeta(Long consumerGroupId);

    Map<Long, ConsumerGroupEntity> getIdCache();

    ConsumerGroupDeleteResponse deleteConsumerGroup(Long consumerGroupId, boolean b);
}
