package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.MqLockEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface MqLockService extends BaseService<MqLockEntity> {
    boolean isMaster();
    boolean updateHeatTime();
    boolean isInLock();
}
