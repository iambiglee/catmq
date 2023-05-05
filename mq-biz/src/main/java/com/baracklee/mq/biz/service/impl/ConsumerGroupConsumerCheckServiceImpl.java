package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupConsumerService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.ConsumerService;
import com.baracklee.mq.biz.service.RedundanceCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author:  BarackLee
 */
@Service
public class ConsumerGroupConsumerCheckServiceImpl implements RedundanceCheckService {


    private ConsumerGroupService consumerGroupService;
    private ConsumerService consumerService;
    private ConsumerGroupConsumerService consumerGroupConsumerService;

    @Autowired
    public ConsumerGroupConsumerCheckServiceImpl(ConsumerGroupService consumerGroupService,
                                                 ConsumerService consumerService,
                                                 ConsumerGroupConsumerService consumerGroupConsumerService) {
        this.consumerGroupService = consumerGroupService;
        this.consumerService = consumerService;
        this.consumerGroupConsumerService = consumerGroupConsumerService;
    }

    @Override
    public String checkItem() {
        return "ConsumerGroupConsumer 下校验 consumerGroup 和 consumer";
    }

    @Override
    public String checkResult() {
        String result = null;
        Map<Long, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getIdCache();
        List<ConsumerEntity> consumerList =consumerService.getList();
        List<ConsumerGroupConsumerEntity> consumerGroupConsumerList=consumerGroupConsumerService.getList();
        Map<Long,ConsumerEntity> consumerMap=new HashMap<>();
        for (ConsumerEntity consumer:consumerList) {
            consumerMap.put(consumer.getId(),consumer);
        }

        result = checkConsumerGroupConsumer(consumerGroupMap, consumerMap, consumerGroupConsumerList);
        return result;
    }

    public String checkConsumerGroupConsumer(Map<Long, ConsumerGroupEntity> consumerGroupMap, Map<Long, ConsumerEntity> consumerMap, List<ConsumerGroupConsumerEntity> consumerGroupConsumerList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (ConsumerGroupConsumerEntity entity : consumerGroupConsumerList) {
            if (!consumerMap.containsKey(entity.getConsumerId())){
                stringBuilder.append("consumer_group_consumer表中Id为："+entity.getId()+"对应的consumer_id不存在"+"<br/>");
            }
            if(!consumerGroupMap.containsKey(entity.getConsumerGroupId())){
                stringBuilder.append("consumer_group_consumer表中Id为："+entity.getId()+"对应的consumer_group_id不存在"+"<br/>");
            }
        }
        return stringBuilder.toString();
    }
}
