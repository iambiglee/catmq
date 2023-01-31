package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import javax.xml.bind.annotation.XmlElementRefs;
import java.util.List;
import java.util.Map;

public interface QueueService extends BaseService<QueueEntity> {
    List<QueueEntity> getQueuesByTopicId(Long topicId);

    long getMaxId(long id, String tbName);

    Map<Long, QueueEntity> getAllQueueMap();
}
