package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.core.IMqMeticsReporterService;
import com.codahale.metrics.MetricFilter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqMeticsReporterService implements IMqMeticsReporterService {
    private AtomicBoolean startFlag= new AtomicBoolean(false);
    private MqMetricsReporter reporter;
    private volatile static MqMeticsReporterService instance=null;

    public static MqMeticsReporterService getInstance(){
        if(instance==null){
            synchronized (MqMeticsReporterService.class){
                if(instance==null){
                    instance=new MqMeticsReporterService();
                }
            }
        }
        return instance;
    }

    private MqMeticsReporterService(){
        reporter=new MqMeticsReporter(MetricSingleton.getMetricRegistry(), "mq-client", MetricFilter.ALL,
                TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS, null, MqClient.getContext());
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            reporter.start(30,TimeUnit.SECONDS);
        }
    }

    @Override
    public void close() {
        startFlag.set(false);
        instance=null;
        if(reporter!=null){
            reporter.stop();
            reporter=null;
        }
    }
}
