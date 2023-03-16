package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dto.LogDto;
import com.baracklee.mq.biz.dto.client.LogRequest;
import com.baracklee.mq.biz.dto.client.OpLogRequest;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

@Service
public class LogServiceImpl implements LogService {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    public LogServiceImpl(ConsumerGroupService consumerGroupService, AuditLogService auditLogService) {
        this.consumerGroupService = consumerGroupService;
        this.auditLogService = auditLogService;
    }

    private ConsumerGroupService consumerGroupService;

    private AuditLogService auditLogService;

    private Map<Integer, String> logType = new HashMap<>();
    {
        logType.put(1, "error");
        logType.put(2, "warn");
        logType.put(3, "info");
        logType.put(4, "debug");
    }


    @Override
    public void addConsumerLog(LogRequest request) {
        if (request == null) {
            return;
        }
        String logContent = getLog(request);
        if (request.getType() >= 3) {
            Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
            if (!StringUtils.isEmpty(request.getConsumerGroupName())
                    && cache.containsKey(request.getConsumerGroupName())) {
                if (cache.get(request.getConsumerGroupName()).getTraceFlag() == 1) {
                    log.info(logContent);
                    // return;
                }
            }
        } else {
            log.info(getLog(request));
        }
    }

    private String getLog(LogRequest log) {
        String rs = "consumerGroupName_" + log.getConsumerGroupName() + "_topic_" + log.getTopicName()
                + "_consumerName_" + log.getConsumerName() + "_action_" + log.getAction() + ",json is "
                + JsonUtil.toJsonNull(log);
        return rs.replaceAll(" ", "_").replaceAll("\\|", "_").replaceAll("\\.", "_") + log.getMsg();
    }

    @Override
    public void addBrokerLog(LogDto request) {
        if (request.getType() >= 3) {
            Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
            if (!StringUtils.isEmpty(request.getConsumerGroupName())
                    && cache.containsKey(request.getConsumerGroupName())) {
                if (cache.get(request.getConsumerGroupName()).getTraceFlag() == 1) {
                    log.info(getLog(request));
                }
            }
        } else if (request.getType() == 1) {
            log.error(getLog(request), request.getThrowable());
        } else if (request.getType() == 2) {
            log.warn(getLog(request));
        }
    }

    @Override
    public void addOpLog(OpLogRequest request) {
        if (request == null || StringUtils.isEmpty(request.getConsumerGroupName())) {
            return;
        }
        Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
        if (!cache.containsKey(request.getConsumerGroupName())) {
            return;
        }
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setContent(request.getContent());
        auditLog.setTbName(ConsumerGroupEntity.TABLE_NAME);
        auditLog.setRefId(cache.get(request.getConsumerGroupName()).getId());
        auditLog.setInsertBy(request.getConsumerName());
        auditLogService.insert(auditLog);
    }
}
