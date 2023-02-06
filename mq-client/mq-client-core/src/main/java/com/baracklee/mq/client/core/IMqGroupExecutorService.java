package com.baracklee.mq.client.core;

import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;

import java.util.Map;

public interface IMqGroupExecutorService extends IMqClientService{
    void rbOrUpdate(ConsumerGroupOneDto consumerGroupOne, String serverIp);
    Map<Long, IMqQueueExecutorService> getQueueEx();
}
