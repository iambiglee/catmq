package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.LogDto;
import com.baracklee.mq.biz.dto.client.LogRequest;
import com.baracklee.mq.biz.dto.client.OpLogRequest;

public interface LogService {
    void addConsumerLog(LogRequest request);

    void addBrokerLog(LogDto requst);

    void addOpLog(OpLogRequest request);
}
