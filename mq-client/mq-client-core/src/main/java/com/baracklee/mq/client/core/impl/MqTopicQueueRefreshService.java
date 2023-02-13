package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.GetTopicQueueIdsRequest;
import com.baracklee.mq.biz.dto.client.GetTopicQueueIdsResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqTopicQueueRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MqTopicQueueRefreshService implements IMqTopicQueueRefreshService {

    private Logger log = LoggerFactory.getLogger(MqBrokerUrlRefreshService.class);
    private ScheduledExecutorService executor = null;
    private GetTopicQueueIdsRequest request = new GetTopicQueueIdsRequest();
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private MqContext mqContext;
    private volatile boolean isStop = false;
    private volatile boolean runStatus = false;
    private AtomicReference<Map<String, List<Long>>> topicQueueRef = new AtomicReference<>(new ConcurrentHashMap<>());
    private long lastTime = System.currentTimeMillis();

    private static MqTopicQueueRefreshService instance=null;

    public static MqTopicQueueRefreshService getInstance() {
        if (instance == null) {
            synchronized (MqTopicQueueRefreshService.class) {
                if (instance == null) instance = new MqTopicQueueRefreshService();
            }
        }
        return instance;
    }

    protected MqTopicQueueRefreshService() {
        this.mqContext = MqClient.getContext();
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            isStop = false;
            runStatus = false;
            executor = Executors.newScheduledThreadPool(1,
                    SoaThreadFactory.create("mq-MqTopicQueueRefreshService-pool-%d", Thread.MAX_PRIORITY - 1, true));
            executor.scheduleAtFixedRate(()->{
                if (!isStop){
                    runStatus=true;
                    if(System.currentTimeMillis()-lastTime>6000){
                        topicQueueRef.set(new ConcurrentHashMap<>());
                    }
                    doUpdateQueue();
                    runStatus=false;
                }
            },1,20, TimeUnit.SECONDS);
        }

    }

    private void doUpdateQueue() {
        if(topicQueueRef.get().size()>0){
            request=new GetTopicQueueIdsRequest();
            request.setTopicNames(new ArrayList<>(topicQueueRef.get().keySet()));
            GetTopicQueueIdsResponse response = mqContext.getMqResource().getTopicQueueIds(request);
            if(response!=null&&response.getTopicQueues()!=null){
                topicQueueRef.get().putAll(response.getTopicQueues());
            }
        }
    }

    @Override
    public void close() {
        isStop = true;
        long start = System.currentTimeMillis();
        // 这是为了等待有未完成的任务
        while (runStatus) {
            Util.sleep(10);
            // System.out.println("closing...................."+isRunning);
            if (System.currentTimeMillis() - start > 5000) {
                break;
            }
        }
        try {
            executor.shutdown();
        } catch (Throwable e) {
        }
        startFlag.set(false);
        executor = null;
        instance = null;
    }

    @Override
    public List<Long> getTopicQueueIds(String topicName) {
        if(Util.isEmpty(topicName)) return new ArrayList<>();
        Map<String, List<Long>> data = topicQueueRef.get();
        if(!data.containsKey(topicName)){
            GetTopicQueueIdsRequest idsRequest = new GetTopicQueueIdsRequest();
            idsRequest.setTopicNames(Collections.singletonList(topicName));
            GetTopicQueueIdsResponse response = mqContext.getMqResource().getTopicQueueIds(idsRequest);
            if(response!=null&&response.getTopicQueues()!=null){
                data.putAll(response.getTopicQueues());
            }
        }
        return data.get(topicName);
    }
}
