package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.service.common.BaseService;


/**
 * 使用数据库记录是谁被选主了
 */
public interface NotifyMessageStatService extends BaseService<NotifyMessageStatEntity> {
    NotifyMessageStatEntity get();
    void updateNotifyMessageId();

    NotifyMessageStatEntity initNotifyMessageStat();
}
