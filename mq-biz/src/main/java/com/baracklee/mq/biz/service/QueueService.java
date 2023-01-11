package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;

public interface QueueService extends BaseService<QueueEntity> {
    List<QueueEntity> getQueuesByTopicId(Long topicId);

    long getMaxId(long id, String tbName);
}
