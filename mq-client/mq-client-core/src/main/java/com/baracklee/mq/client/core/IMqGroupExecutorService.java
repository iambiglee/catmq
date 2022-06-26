package com.baracklee.mq.client.core;

import com.baracklee.mq.biz.dto.client.ConsumerGroupOneDto;

import java.util.Map;

public interface IMqGroupExecutorService extends IMqClientService{
    void rbOrUpdate(ConsumerGroupOneDto consumerGroupOne, String serverIp);
    Map<Long, IMqQueueExcutorService> getQueueEx();
}
