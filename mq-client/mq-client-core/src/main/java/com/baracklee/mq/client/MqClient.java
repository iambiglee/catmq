package com.baracklee.mq.client;

import com.baracklee.mq.biz.event.PreHandleListener;

public class MqClient {

    private static MqContext mqContext = new MqContext();
    private static MqEnvironment mqEnvironment=null;
    private static Object lockObj = new Object();


    public static MqContext getContext() {
        return mqContext;
    }

    public static MqEnvironment getMqEnvironment() {
        return mqEnvironment;
    }

    public static void setMqEnvironment(MqEnvironment mqEnvironment) {
        MqClient.mqEnvironment = mqEnvironment;
        getContext().setMqEnvironment(mqEnvironment);
    }

    public static ISubscriberResolver getSubscriberResolver() {
        return subscriberResolver;
    }

    public static void setSubscriberResolver(ISubscriberResolver subscriberResolver) {
        MqClient.subscriberResolver = subscriberResolver;
    }
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                close();
            }
        });
    }
    public static void registerPreHandleEvent(PreHandleListener preHandleListener1) {
        synchronized (lockObj) {
            getContext().getMqEvent().setPreHandleListener(preHandleListener1);
        }
    }

    public static boolean start() {
        if (startFlag.compareAndSet(false, true)) {
            registerConsumerGroup();
        }
        return false;
    }

}
