package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.AuditLogRepository;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.service.UserInfoHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)
public class AuditLogServiceImplTest {

    @Test
    public void insetTest(){
        UserInfoHolder userInfoHolder=mock(UserInfoHolder.class);
        AuditLogRepository auditLogRepository=mock(AuditLogRepository.class);
        AuditLogServiceImpl auditLogServiceImpl=new AuditLogServiceImpl(auditLogRepository,userInfoHolder);
        auditLogServiceImpl.init();
        AuditLogEntity entity=new AuditLogEntity();
        entity.setContent("test");
        entity.setId(1L);
        entity.setRefId(1L);
        auditLogServiceImpl.insert(entity);
        verify(auditLogRepository).insert(entity);
    }

}
