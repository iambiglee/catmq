package com.baracklee.mq.biz.cache;

import com.baracklee.mq.biz.common.inf.ConsumerGroupChangedListener;
import com.baracklee.mq.biz.dto.base.ConsumerGroupDto;

import java.util.Map;

public class ConsumerGroupCacheServiceImpl implements ConsumerGroupCacheService {
    @Override
    public void addListen(ConsumerGroupChangedListener listener) {

    }

    @Override
    public Map<String, ConsumerGroupDto> getCache() {
        return null;
    }

    @Override
    public void startBroker() {

    }

    @Override
    public void stopBroker() {

    }

    @Override
    public String info() {
        return null;
    }
}
