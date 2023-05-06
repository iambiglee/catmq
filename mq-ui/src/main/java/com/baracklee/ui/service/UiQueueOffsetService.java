package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
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
}
