package com.baracklee.mq.client.resolver;

import com.baracklee.mq.biz.event.IAsynSubscriber;
import com.baracklee.mq.biz.event.ISubscriber;

public interface ISubscriberResolver {
    IAsynSubscriber getAsnySubscriber(String className) throws Exception;
    ISubscriber getSubscriber(String className) throws Exception;

}
