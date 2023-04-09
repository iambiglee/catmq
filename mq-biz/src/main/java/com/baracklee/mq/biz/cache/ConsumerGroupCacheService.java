package com.baracklee.mq.biz.cache;

import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.ConsumerGroupChangedListener;
import com.baracklee.mq.biz.dto.base.ConsumerGroupDto;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ConsumerGroupCacheService extends BrokerTimerService {

    void addListener(ConsumerGroupChangedListener listener);

    Map<String, ConsumerGroupDto> getCache();
}
