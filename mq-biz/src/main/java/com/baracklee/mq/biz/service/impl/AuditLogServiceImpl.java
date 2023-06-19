package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.AuditLogRepository;
import com.baracklee.mq.biz.dto.request.AuditLogRequest;
import com.baracklee.mq.biz.dto.response.AuditLogResponse;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditLogServiceImpl extends AbstractBaseService<AuditLogEntity> implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Autowired
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserInfoHolder userInfoHolder) {
        this.auditLogRepository = auditLogRepository;
        this.userInfoHolder = userInfoHolder;
    }

    private final AuditLogRepository auditLogRepository;
    private final UserInfoHolder userInfoHolder;


    @PostConstruct
   public void init(){
       super.setBaseRepository(auditLogRepository);
   }

    @Override
    public long insert(AuditLogEntity entity) {
        log.info("tbname_{}_refid_{},content is {},by {}", entity.getTbName(), entity.getRefId(),
                entity.getContent().replaceAll(" ", "_").replaceAll("\\|", "_").replaceAll("\\.", "_"),
                entity.getInsertBy());
        try {
            if(entity.getContent()!=null&&entity.getContent().length()>5000) {
                log.info("log_insert_spec");
                entity.setContent(entity.getContent().substring(0,5000));
            }
            return super.insert(entity);
        } catch (Exception e) {
            log.error("log_insert", e);
            return 0;
        }
    }

    @Override
    public long getMindId() {
        Long minId = auditLogRepository.getMinId();
        if(minId==null) return 0L;
        else return minId;
    }

    @Override
    public void deleteBy(Long minId) {
        auditLogRepository.deleteBy(minId);
    }

    @Override
    public void recordAudit(String tbName, long refId, String content) {
        String userId=userInfoHolder.getUserId();
        AuditLogEntity auditLogVo = new AuditLogEntity(tbName, refId, content, userId, userId);
        insert(auditLogVo);
    }

    @Override
    public AuditLogResponse logList(AuditLogRequest auditLogRequest) {
        Map<String, Object> conditionMap = new HashMap<>();
        if (StringUtils.isNotEmpty(auditLogRequest.getTbName())) {
            conditionMap.put(AuditLogEntity.FdTbName, auditLogRequest.getTbName());
        }
        if (StringUtils.isNotEmpty(auditLogRequest.getRefId())) {
            conditionMap.put(AuditLogEntity.FdRefId, auditLogRequest.getRefId());
        }
        if (StringUtils.isNotEmpty(auditLogRequest.getContent())) {
            conditionMap.put(AuditLogEntity.FdContent, auditLogRequest.getContent());
        }
        if (StringUtils.isNotEmpty(auditLogRequest.getId())) {
            conditionMap.put(AuditLogEntity.FdId, Long.valueOf(auditLogRequest.getId()));
        }
        Long count = count(conditionMap);
        if (count == 0) {
            return new AuditLogResponse(count, null);
        }
        return new AuditLogResponse(count, getList(conditionMap, Long.parseLong(auditLogRequest.getPage()),
                Long.parseLong(auditLogRequest.getLimit())));

    }
}
