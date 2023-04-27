package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.ServerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;

public interface ServerService extends BaseService<ServerEntity> {
    List<String> getBrokerUrlCache();
    int getOnlineServerNum();
    void batchUpdate(List<Long> serverIds,int serverStatus);


}
