package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.service.RedundanceCheckService;
import com.baracklee.mq.biz.service.TopicService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:  BarackLee
 * 检查：consumerGroup 和 topic 表数据是否一致
 黑白名单只能存在一个，originName是否和自己一致，
 consumer_quality不能小于0，
 topicNames订阅检查（数量和值），
 是否只存在虚拟消费者组，不存在原始组。
 */
@Service
public class ConsumerGroupCheckServiceImpl implements RedundanceCheckService {

    private ConsumerGroupService consumerGroupService;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private TopicService topicService;

    @Autowired
    public ConsumerGroupCheckServiceImpl(ConsumerGroupService consumerGroupService,
                                         ConsumerGroupTopicService consumerGroupTopicService,
                                         TopicService topicService) {
        this.consumerGroupService = consumerGroupService;
        this.consumerGroupTopicService = consumerGroupTopicService;
        this.topicService = topicService;
    }

    @Override
    public String checkItem() {
        String field = "ConsumerGroup下校验:" + ConsumerGroupEntity.FdName + "," + ConsumerGroupEntity.FdOriginName + "," +
                ConsumerGroupEntity.FdIpWhiteList + "," + ConsumerGroupEntity.FdIpBlackList + "," + ConsumerGroupEntity.FdTopicNames;
        return field;
    }

    @Override
    public String checkResult() {
        String result = null;
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        Map<String, ConsumerGroupTopicEntity> consumerGroupTopicMap = consumerGroupTopicService.getGroupTopic();
        Map<String, TopicEntity> topicMap=topicService.getCache();
        result = checkConsumerGroup(consumerGroupMap, consumerGroupTopicMap,topicMap);
        return result;
    }

    public String checkConsumerGroup(Map<String, ConsumerGroupEntity> consumerGroupMap,
                                      Map<String, ConsumerGroupTopicEntity> consumerGroupTopicMap,
                                      Map<String, TopicEntity> topicMap) {
        StringBuilder resultBuilder = new StringBuilder();
        Map<String,String> consumerGroupCheck=new HashMap<>(consumerGroupMap.size());
        for (Map.Entry<String, ConsumerGroupEntity> groupEntityEntry : consumerGroupMap.entrySet()) {
            String groupName = groupEntityEntry.getKey();
            ConsumerGroupEntity consumerGroupEntity = groupEntityEntry.getValue();

            //检查consumerGroup 表和 topic 关联表
            if(StringUtils.isNotEmpty(consumerGroupEntity.getTopicNames())) {
                String[] subTopicNames = consumerGroupEntity.getTopicNames().split(",");
                for (String topicName : subTopicNames) {
                    if(!consumerGroupTopicMap.containsKey(groupName+"_"+topicName)){
                        resultBuilder.append("consumer_group表中的：").append(groupName)
                                .append("订阅的topic：").append(topicName)
                                .append("在consumer_group_topic中不存在，建议修复consumer_group表中的：")
                                .append(groupName).append("的topic_names字段").append("<br/>");
                    }
                }

                for (String topicName:subTopicNames) {
                    if(!topicMap.containsKey(topicName)){
                        resultBuilder.append("consumer_group表中的：")
                                .append(groupName).append("订阅的topic：")
                                .append(topicName).append("在Topic表中不存在建议修复consumer_group表中的：")
                                .append(groupName).append("的topic_names字段").append("<br/>");
                    }
                }

            }

            //黑白名单的逻辑检查
            if(StringUtils.isNotEmpty(consumerGroupEntity.getIpWhiteList())
                    &&StringUtils.isNotEmpty(consumerGroupEntity.getIpBlackList()))
            {
                resultBuilder.append("consumer_Group表中").append(groupName).append("同时存在黑白名单");
            }

            if (consumerGroupEntity.getMode() == 1&&!groupName.equals(consumerGroupEntity.getOriginName())) {
                resultBuilder.append("consumer_group表中的：")
                        .append(groupName).append("与它的origin_name：")
                        .append(consumerGroupMap.get(groupName).getOriginName()).append("不一致").append("<br/>");
            }

            if (consumerGroupEntity.getConsumerQuality() < 0) {
                resultBuilder.append("consumer_group表中的：").append(groupName).append("consumer_quality不能小于0").append("<br/>");
            }

            //对于广播消费者组
            if (consumerGroupEntity.getMode() == 2 && !groupName.equals(consumerGroupEntity.getOriginName())) {
                //对于镜像消费者组
                if (!consumerGroupMap.containsKey(consumerGroupMap.get(groupName).getOriginName())) {
                    resultBuilder.append("consumer_group表中的镜像组：").append(groupName).append("的原始组：").append(consumerGroupMap.get(groupName).getOriginName()).append("不存在，建议删除！").append("<br/>");
                }
            }

            if(consumerGroupCheck.containsKey(groupName.toLowerCase())){
                resultBuilder.append("consumer_group表中的：").append(groupName).append("重复（名称不区分大小写）").append("<br/>");
            }else{
                consumerGroupCheck.put(groupName.toLowerCase(),"");
            }

        }
        return resultBuilder.toString();
    }
}
