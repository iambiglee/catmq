package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface NotifyMessageService extends BaseService<NotifyMessageEntity> {

    long getDataMaxId(long maxId1);

    long getDataMaxId();

    long getDataMinId();

    long getRbMaxId(long maxId1);

    long getRbMaxId();

    long getRbMinId();

    int clearOld(long clearOldTime, long maxId);

    long getMinId();
}
