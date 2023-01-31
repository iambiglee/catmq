package com.baracklee.mq.biz.event;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;

public interface PreHandleListener {

    //如果返回False,表示暂停当前线程
    boolean preHandle(ConsumerQueueDto consumerQueueDto);
}
