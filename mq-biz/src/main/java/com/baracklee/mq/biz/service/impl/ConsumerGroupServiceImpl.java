package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ConsumerGroupServiceImpl extends AbstractBaseService<ConsumerGroupEntity>
        implements CacheUpdateService, ConsumerGroupService{
    protected AtomicReference<Map<String, ConsumerGroupEntity>> consumerGroupRefMap = new AtomicReference<>(
            new HashMap<>());
    @Override
    public Map<String, ConsumerGroupEntity> getCache() {
        Map<String, ConsumerGroupEntity> rs = consumerGroupRefMap.get();
        
        
        return null;
    }
}
