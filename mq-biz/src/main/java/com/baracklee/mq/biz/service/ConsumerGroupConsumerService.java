package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public interface ConsumerGroupConsumerService extends BaseService<ConsumerGroupConsumerEntity> {

    int deleteUnActiveConsumer();

    List<ConsumerGroupConsumerEntity> getByConsumerGroupIds(ArrayList<Long> consumerGroupIds);
}
