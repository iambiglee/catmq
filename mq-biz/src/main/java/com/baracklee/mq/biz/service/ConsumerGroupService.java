package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterResponse;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.Map;

public interface ConsumerGroupService extends BaseService<ConsumerEntity> {
    Map<String, ConsumerGroupEntity> getCache();
}
