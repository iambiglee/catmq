package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.Map;

public interface ConsumerGroupTopicService extends BaseService<ConsumerGroupTopicEntity> {
    Map<Long, Map<String, ConsumerGroupTopicEntity>> getCache();
}
