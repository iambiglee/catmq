package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface ConsumerService extends BaseService<ConsumerEntity> {
    List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerGroupIds(List<Long> consumerGroupIds);
    List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerIds(List<Long> consumerIds);
    ConsumerRegisterResponse register(ConsumerRegisterRequest request);
    ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request);
    ConsumerDeRegisterResponse deRegister(ConsumerDeRegisterRequest deRegisterRequest);
    PublishMessageResponse publish(PublishMessageRequest request);
    PullDataResponse pullData(PullDataRequest request);
    FailMsgPublishAndUpdateResultResponse publishAndUpdateResultFailMsg(FailMsgPublishAndUpdateResultRequest request);
    GetMessageCountResponse getMessageCount(GetMessageCountRequest request);
    int heartbeat(List<Long> ids);
    List<ConsumerEntity> findByHeartTimeInterval(long heartTimeInterval);
    boolean deleteByConsumers(List<ConsumerEntity>consumers);
    ConsumerEntity getConsumerByConsumerGroupId(Long consumerGroupId);
    long countBy(Map<String, Object> conditionMap);
    List<ConsumerEntity> getListBy(Map<String, Object> conditionMap);
}
