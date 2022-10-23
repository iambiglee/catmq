package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.ConsumerGroupConsumerRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.ConsumerGroupConsumerService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ConsumerGroupConsumerServiceImpl extends AbstractBaseService<ConsumerGroupConsumerEntity> implements ConsumerGroupConsumerService {
    @Autowired
    private ConsumerGroupConsumerRepository consumerGroupConsumerRepository;
    @Override
    public int deleteUnActiveConsumer() {
        return consumerGroupConsumerRepository.deleteUnActiveConsumer();
    }

    @Override
    public List<ConsumerGroupConsumerEntity> getByConsumerGroupIds(ArrayList<Long> consumerGroupIds) {
        if(CollectionUtils.isEmpty(consumerGroupIds)) return new ArrayList<>();
        return consumerGroupConsumerRepository.getByConsumerGroupIds(consumerGroupIds);
    }
}
