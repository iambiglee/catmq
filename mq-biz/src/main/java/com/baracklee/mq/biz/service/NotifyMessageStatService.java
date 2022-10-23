package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface NotifyMessageStatService extends BaseService<NotifyMessageStatEntity> {
    NotifyMessageStatEntity get();
    void updateNotifyMessageId();

    NotifyMessageStatEntity initNotifyMessageStat();
}
