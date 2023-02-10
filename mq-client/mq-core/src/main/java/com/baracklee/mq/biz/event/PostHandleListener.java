package com.baracklee.mq.biz.event;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;

public interface PostHandleListener {
    //如果返回False,表示暂停当前线程
    boolean postHandle(ConsumerQueueDto consumerQueueDto,Boolean isSuc);
}
