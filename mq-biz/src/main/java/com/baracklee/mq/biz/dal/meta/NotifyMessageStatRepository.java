package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;

public interface NotifyMessageStatRepository extends BaseRepository<NotifyMessageStatEntity> {
    void updateNotifyMessageId();

}
