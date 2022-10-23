package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface NotifyMessageService extends BaseService<NotifyMessageEntity> {
    long getRbMaxId(long lastNotifyMessageId);
    long getRbMaxId();

    long getRbMinId();
}
