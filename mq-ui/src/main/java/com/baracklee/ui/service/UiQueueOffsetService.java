package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Barack Lee
 */
@Service
public class UiQueueOffsetService {

    private QueueOffsetService queueOffsetService;

    private AuditLogService auditLogService;

    private ConsumerGroupService consumerGroupService;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private UserInfoHolder userInfoHolder;

    public void deleteByQueueId(long queueId, Long topicId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueOffsetEntity.FdQueueId, queueId);
        List<QueueOffsetEntity> queueOffsetEntityList = queueOffsetService.getList(conditionMap);
        List<Long> ids = queueOffsetEntityList.stream().map(QueueOffsetEntity::getId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(ids)){
            queueOffsetService.delete(ids);
            auditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                    "移除queue[" + queueId + "]时，删除queueoffset, " + JsonUtil.toJson(queueOffsetEntityList));        }
    }

    public void createQueueOffsetForExpand(QueueEntity queueEntity, Long topicId, TopicEntity topicEntity) {
        Map<String, Object> conditionMap = new HashMap<>();
        Map<String,ConsumerGroupEntity> consumerGroupMap=consumerGroupService.getCache();
        conditionMap.put(ConsumerGroupTopicEntity.FdTopicId, topicId);
        List<ConsumerGroupTopicEntity> consumerGroupTopicServiceList = consumerGroupTopicService.getList(conditionMap);
        List<QueueOffsetEntity> queueOffsetEntityList = new ArrayList<>();
        consumerGroupTopicServiceList.forEach(consumerGroupTopicEntity -> {
            QueueOffsetEntity queueOffsetEntity = new QueueOffsetEntity();
            queueOffsetEntity.setConsumerGroupId(consumerGroupTopicEntity.getConsumerGroupId());
            queueOffsetEntity.setConsumerGroupName(consumerGroupTopicEntity.getConsumerGroupName());
            //为queueOffset添加origin_consumer_group_name字段
            queueOffsetEntity.setOriginConsumerGroupName(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getOriginName());
            queueOffsetEntity.setConsumerGroupMode(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getMode());
            queueOffsetEntity.setTopicId(topicId);
            queueOffsetEntity.setTopicName(consumerGroupTopicEntity.getTopicName());
            queueOffsetEntity.setOriginTopicName(consumerGroupTopicEntity.getOriginTopicName());
            queueOffsetEntity.setTopicType(consumerGroupTopicEntity.getTopicType());
            queueOffsetEntity.setQueueId(queueEntity.getId());
            queueOffsetEntity.setSubEnv(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getSubEnv());
            queueOffsetEntity
                    .setDbInfo(queueEntity.getIp() + " | " + queueEntity.getDbName() + " | " + queueEntity.getTbName());
            String userId = userInfoHolder.getUserId();
            queueOffsetEntity.setInsertBy(userId);
            queueOffsetEntityList.add(queueOffsetEntity);
        });
        queueOffsetService.insertBatch(queueOffsetEntityList);
        // 扩容时，把扩容信息同步到 mq2
        //synService32.synTopicExpand(topicEntity, queueEntity, queueOffsetEntityList);

    }

    public List<QueueOffsetEntity> findByQueueId(Long queueId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueOffsetEntity.FdQueueId, queueId);
        return queueOffsetService.getList(conditionMap);
    }
}
