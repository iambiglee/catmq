package com.baracklee.mq.biz.dto.response;


import com.baracklee.mq.biz.entity.AuditLogEntity;

import java.util.List;

public class AuditLogResponse extends BaseUiResponse<List<AuditLogEntity>> {

    public AuditLogResponse(Long count, List<AuditLogEntity> data) {
        super(count, data);
    }
}
