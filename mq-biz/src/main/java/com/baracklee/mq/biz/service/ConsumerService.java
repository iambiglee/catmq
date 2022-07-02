package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterResponse;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.service.common.BaseService;

public interface ConsumerService extends BaseService<ConsumerEntity> {
    ConsumerRegisterResponse register(ConsumerRegisterRequest request);
}
