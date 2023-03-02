package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.ConsumerGroupConsumerRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.service.ConsumerGroupConsumerService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConsumerGroupConsumerServiceImpl extends AbstractBaseService<ConsumerGroupConsumerEntity> implements ConsumerGroupConsumerService {
    @Autowired
    private ConsumerGroupConsumerRepository consumerGroupConsumerRepository;
    @Override
    public int deleteUnActiveConsumer() {
        return consumerGroupConsumerRepository.deleteUnActiveConsumer();
    }

    @Override
    public List<ConsumerGroupConsumerEntity> getByConsumerGroupIds(List<Long> consumerGroupIds) {
        if(CollectionUtils.isEmpty(consumerGroupIds)) return new ArrayList<>();
        return consumerGroupConsumerRepository.getByConsumerGroupIds(consumerGroupIds);
    }

    @Override
    public List<ConsumerGroupConsumerEntity> getByConsumerIds(List<Long> consumerIds) {
        if(CollectionUtils.isEmpty(consumerIds)) return new ArrayList<>();
        return consumerGroupConsumerRepository.getByConsumerGroupIds(consumerIds);
    }

    @Override
    public void deleteByConsumerIds(List<Long> consumerIds) {
        consumerGroupConsumerRepository.deleteByConsumerIds(consumerIds);
    }
}
