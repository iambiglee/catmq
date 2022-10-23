package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.NotifyMessageStatRepository;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.service.NotifyMessageStatService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotifyMessageStatServiceImpl extends AbstractBaseService<NotifyMessageStatEntity> implements NotifyMessageStatService {
    @Resource
    NotifyMessageStatRepository notifyMessageStatRepository;


    @Override
    public NotifyMessageStatEntity get() {
        Map<String, Object> conMap = new HashMap<>();
        conMap.put("key1", "rb_notifyMessageStat");
        NotifyMessageStatEntity messageStatEntity = notifyMessageStatRepository.get(conMap);
        return messageStatEntity;    }

    @Override
    public void updateNotifyMessageId() {
        notifyMessageStatRepository.updateNotifyMessageId();
    }

    @Override
    public NotifyMessageStatEntity initNotifyMessageStat() {
        NotifyMessageStatEntity messageStatEntity = new NotifyMessageStatEntity();
        messageStatEntity.setKey1("rb_notifyMessageStat");
        messageStatEntity.setNotifyMessageId(0);
        notifyMessageStatRepository.insert(messageStatEntity);
        return messageStatEntity;
    }
}
