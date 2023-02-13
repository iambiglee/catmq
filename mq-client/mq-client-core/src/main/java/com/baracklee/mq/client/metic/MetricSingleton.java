package com.baracklee.mq.client.metic;

import com.codahale.metrics.MetricRegistry;

public class MetricSingleton {
    private MetricSingleton() {}

    private static class SingletonHelper{
        private static final MetricRegistry INSTANCE=new MetricRegistry();
    }

    public static MetricRegistry getMetricRegistry(){
        return SingletonHelper.INSTANCE;
    }
}
