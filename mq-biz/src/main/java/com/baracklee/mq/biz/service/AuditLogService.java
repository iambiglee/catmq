package com.baracklee.mq.biz.service;


import com.baracklee.mq.biz.dto.request.AuditLogRequest;
import com.baracklee.mq.biz.dto.response.AuditLogResponse;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.service.common.BaseService;

/**
 * @author dal-generator
 */
public interface AuditLogService extends BaseService<AuditLogEntity> {
	long getMindId();
    void deleteBy(Long minId);
    void recordAudit(String tbName, long refId, String content);
    AuditLogResponse logList(AuditLogRequest auditLogRequest);
}
