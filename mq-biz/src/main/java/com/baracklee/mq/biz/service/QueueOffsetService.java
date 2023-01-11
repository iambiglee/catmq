package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface QueueOffsetService extends BaseService<QueueOffsetEntity> {
    void updateConsumerId(QueueOffsetEntity t1);

    List<QueueOffsetEntity> getByConsumerGroupIds(ArrayList<Long> consumerGroupIds);

    void createQueueOffset(ConsumerGroupTopicEntity consumerGroupTopicEntity);

    Map<String, QueueOffsetEntity> getUqCache();

    void updateCache();

    void forceUpdateCache();
}
