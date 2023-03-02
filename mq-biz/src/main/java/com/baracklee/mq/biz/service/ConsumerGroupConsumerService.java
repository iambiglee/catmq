package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;

public interface ConsumerGroupConsumerService extends BaseService<ConsumerGroupConsumerEntity> {

    int deleteUnActiveConsumer();

    List<ConsumerGroupConsumerEntity> getByConsumerGroupIds(List<Long> consumerGroupIds);

    List<ConsumerGroupConsumerEntity> getByConsumerIds(List<Long> consumerIds);

    void deleteByConsumerIds(List<Long> consumerIds);
}
