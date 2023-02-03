package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.trace.TraceFactory;
import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupRequest;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IConsumerPollingService;
import com.baracklee.mq.client.core.IMqGroupExecutorService;
import com.baracklee.mq.client.factory.IMqFactory;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsumerPollingService implements IConsumerPollingService {
    private Logger log = LoggerFactory.getLogger(ConsumerPollingService.class);
    private ThreadPoolExecutor executor = null;
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private Map<String, IMqGroupExcutorService> mqExcutors = new ConcurrentHashMap<>();
    private MqContext mqContext = null;
    private IMqResource mqResource;
    private IMqFactory mqFactory;
    private volatile boolean isStop = false;
    private volatile boolean runStatus = false;

    public ConsumerPollingService() {
        this(MqClient.getMqFactory().createMqResource(MqClient.getContext().getConfig().getUrl(), 32000, 32000));
    }

    public ConsumerPollingService(IMqResource mqResource) {
        this.mqContext = MqClient.getContext();
        this.mqResource = mqResource;
        this.mqFactory = MqClient.getMqFactory();
        this.mqContext.setMqPollingResource(mqResource);
    }

    @Override
    public Map<String, IMqGroupExecutorService> getMqExecutors() {
        return mqExcutors;
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            isStop=true;
            runStatus=false;
            executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100),
                    SoaThreadFactory.create("ConsumerPollingService", true),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            executor.execute(()->{
                while (!isStop){
                    runStatus=true;
                    longPolling();
                    runStatus=false;
                }
            });
        }

    }

    private void longPolling() {
        if(mqContext.getConsumerId()>0&&mqContext.getConsumerGroupVersion()!=null
        &&mqContext.getConsumerGroupVersion().size()>0){
            GetConsumerGroupRequest request = new GetConsumerGroupRequest();
            request.setConsumerId(mqContext.getConsumerId());
            request.setConsumerGroupVersion(mqContext.getConsumerGroupVersion());
            GetConsumerGroupResponse response = mqResource.getConsumerGroup(request);
            if (response != null && response.getConsumerDeleted() == 1) {
                log.info("consumerid为" + request.getConsumerId());
            }
            handleGroup(response);
        }else {
            Util.sleep(1000);
        }
    }

    @Override
    public void close() {

    }
}
