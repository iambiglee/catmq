package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.NotifyMessageRepository;
import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MessageType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class NotifyMessageServiceImpl extends AbstractBaseService<NotifyMessageEntity> implements NotifyMessageService {

    @Resource
    private NotifyMessageRepository notifyMessageRepository;
    @Override
    public long getRbMaxId(long lastNotifyMessageId) {
        Long maxId1 = notifyMessageRepository.getMaxId(lastNotifyMessageId, MessageType.Rb);
        if (maxId1 == null) {
            return 0;
        }
        return maxId1;    }

    @Override
    public long getRbMaxId() {
        Long maxId1 = notifyMessageRepository.getMaxId1(MessageType.Rb);
        if (maxId1 == null) {
            return 0;
        }
        return maxId1;    }

    @Override
    public long getRbMinId() {
        Long mindId=notifyMessageRepository.getMinId(MessageType.Rb);
        if (mindId==null) return 0;
        return mindId;
    }
}
