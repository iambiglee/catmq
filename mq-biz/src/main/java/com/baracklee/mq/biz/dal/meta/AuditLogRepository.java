package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogRepository extends BaseRepository<AuditLogEntity> {
    Long getMinId();
    void deleteBy(Long minId);
}
