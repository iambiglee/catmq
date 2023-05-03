package com.baracklee.ui.service;

import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.NotifyMessageStatService;
import com.baracklee.mq.biz.ui.dto.response.MessageNotifyResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageStatNotifyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author： Barack Lee
 */
@Service
public class UiNotifyService {
    private NotifyMessageService notifyMessageService;

    private NotifyMessageStatService notifyMessageStatService;

    private AuditLogService auditLogService;

    @Autowired
    public UiNotifyService(NotifyMessageService notifyMessageService,
                           NotifyMessageStatService messageStatService,
                           AuditLogService auditLogService) {
        this.notifyMessageService = notifyMessageService;
        this.notifyMessageStatService = messageStatService;
        this.auditLogService = auditLogService;
    }

    public MessageNotifyResponse getNotifyMessageByPage(long page, long limit){
        Map<String,Object> parameter=new HashMap<>();
        Long count=notifyMessageService.count(parameter);
        List<NotifyMessageEntity> notifyMessageEntities=notifyMessageService.getList(parameter,page,limit);
        return new MessageNotifyResponse(count,notifyMessageEntities);
    }
    public MessageStatNotifyResponse getNotifyKey(long page, long limit){
        Map<String,Object> parameter=new HashMap<>();
        Long count=notifyMessageStatService.count(parameter);
        List<NotifyMessageStatEntity> notifyMessageStatEntities=notifyMessageStatService.getList(parameter,page,limit);
        return new MessageStatNotifyResponse(count,notifyMessageStatEntities);
    }

    public void updateNotifyMessageStat(long id,long notityMessageId){
        NotifyMessageStatEntity notifyMessageStatEntity=notifyMessageStatService.get(id);
        notifyMessageStatEntity.setNotifyMessageId(notityMessageId);
        notifyMessageStatService.update(notifyMessageStatEntity);
        auditLogService.recordAudit(NotifyMessageStatEntity.TABLE_NAME, notifyMessageStatEntity.getId(), "更改notifyMessageStatEntity");
    }

}
