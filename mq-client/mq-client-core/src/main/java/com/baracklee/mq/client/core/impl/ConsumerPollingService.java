package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.trace.TraceFactory;
import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupRequest;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IConsumerPollingService;
import com.baracklee.mq.client.core.IMqClientService;
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
    private Map<String, IMqGroupExecutorService> mqExcutors = new ConcurrentHashMap<>();
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
            isStop=false;
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

    private void handleGroup(GetConsumerGroupResponse response) {
        if(isStop) return;
        if(response!=null){
            mqContext.setBrokerMetaMode(response.getBrokerMetaMode());
        }
        if(response!=null&&response.getConsumerGroups()!=null&&response.getConsumerGroups().size()>0){
            log.info("consumer_polling 获取到的最新消费者组为"+ JsonUtil.toJsonNull(response));
            response.getConsumerGroups().forEach((key, value) -> {
                if(!isStop){
                    if(!mqExcutors.containsKey(key)){
                        mqExcutors.put(key,mqFactory.createMqGroupExecutorService());
                    }
                    log.info("consumer_group_data_change,消费者组" + key + "发生重平衡或者meta更新");
                    //重平衡基础的元数据
                    mqExcutors.get(key).rbOrUpdate(value,response.getServerIp());
                    mqContext.getConsumerGroupVersion().put(key,value.getMeta().getVersion());
                }
            });
        }
        mqExcutors.values().forEach(IMqClientService::start);
    }

    @Override
    public void close() {
        isStop=true;
        mqExcutors.values().forEach(IMqClientService::close);
        mqExcutors.clear();
        executor.shutdown();
        startFlag.set(false);
        isStop=true;
    }
}
