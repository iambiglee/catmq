package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;

public interface ConsumerService extends BaseService<ConsumerEntity> {
    ConsumerRegisterResponse register(ConsumerRegisterRequest request);

    ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request);

    List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerGroupIds(List<Long> consumerGroupIds);

    ConsumerDeRegisterResponse deRegister(ConsumerDeRegisterRequest request);

    PublishMessageResponse publish(PublishMessageRequest request);
}
