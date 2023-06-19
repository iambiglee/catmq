package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.AuditLogRepository;
import com.baracklee.mq.biz.dto.request.AuditLogRequest;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.service.UserInfoHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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

    @Test
    public void getMindIdTest(){
        UserInfoHolder userInfoHolder=mock(UserInfoHolder.class);
        AuditLogRepository auditLogRepository=mock(AuditLogRepository.class);
        AuditLogServiceImpl auditLogServiceImpl=new AuditLogServiceImpl(auditLogRepository,userInfoHolder);
        auditLogServiceImpl.init();
        when(auditLogRepository.getMinId()).thenReturn(null);
        assertEquals(0L,  auditLogServiceImpl.getMindId());
        when(auditLogRepository.getMinId()).thenReturn(1L);
        assertEquals(1L,  auditLogServiceImpl.getMindId());
    }

    @Test
    public void deleteByTest(){
        UserInfoHolder userInfoHolder=mock(UserInfoHolder.class);
        AuditLogRepository auditLogRepository=mock(AuditLogRepository.class);
        AuditLogServiceImpl auditLogServiceImpl=new AuditLogServiceImpl(auditLogRepository,userInfoHolder);
        auditLogServiceImpl.init();
        doNothing().when(auditLogRepository).delete(0);
        auditLogServiceImpl.delete(0);
        verify(auditLogRepository).delete(anyLong());
    }

    @Test
    public void logListTest(){
        UserInfoHolder userInfoHolder=mock(UserInfoHolder.class);
        AuditLogRepository auditLogRepository=mock(AuditLogRepository.class);
        AuditLogServiceImpl auditLogServiceImpl=new AuditLogServiceImpl(auditLogRepository,userInfoHolder);
        auditLogServiceImpl.init();
        AuditLogRequest request = new AuditLogRequest();
        request.setTbName("test");
        request.setContent("tet");
        request.setLimit("1");
        request.setPage("1");
        request.setRefId("1");
        when(auditLogRepository.count(any(Map.class))).thenReturn(0L);
        assertEquals(0L, (long)(auditLogServiceImpl.logList(request).getCount()));
        when(auditLogRepository.count(any(Map.class))).thenReturn(1L);
        assertEquals(1L, (long)(auditLogServiceImpl.logList(request).getCount()));
    }
}
