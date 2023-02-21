package com.baracklee.mq.client.bootStrap;

import com.baracklee.mq.biz.event.IAsynSubscriber;
import com.baracklee.mq.biz.event.ISubscriber;
import com.baracklee.mq.client.MqSpringUtil;
import com.baracklee.mq.client.resolver.ISubscriberResolver;

public class SubscriberResolver implements ISubscriberResolver {
    @Override
    public IAsynSubscriber getAsnySubscriber(String className) throws Exception {
        Class<IAsynSubscriber> onwClass = (Class<IAsynSubscriber>) Class.forName(className);
        IAsynSubscriber iAsynSubscriber = (IAsynSubscriber) MqSpringUtil.getBean(className);
        if (iAsynSubscriber == null) {
            if (IAsynSubscriber.class.isAssignableFrom(onwClass)) {
                iAsynSubscriber = (IAsynSubscriber) onwClass.newInstance();
            }
        }
        if (iAsynSubscriber == null) {
            throw new Exception(className + " 不存在!");
        }
        return iAsynSubscriber;
    }

    @Override
    public ISubscriber getSubscriber(String className) throws Exception {
        Class<IAsynSubscriber> onwClass = (Class<IAsynSubscriber>) Class.forName(className);
        ISubscriber iAsynSubscriber = (ISubscriber) MqSpringUtil.getBean(className);
        if (iAsynSubscriber == null) {
            if (IAsynSubscriber.class.isAssignableFrom(onwClass)) {
                iAsynSubscriber = (ISubscriber) onwClass.newInstance();
            }
        }
        if (iAsynSubscriber == null) {
            throw new Exception(className + " 不存在!");
        }
        return iAsynSubscriber;
    }
}
