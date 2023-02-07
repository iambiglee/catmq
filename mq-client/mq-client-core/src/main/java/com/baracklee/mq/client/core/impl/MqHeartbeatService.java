package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.HeartbeatRequest;
import com.baracklee.mq.biz.dto.client.HeartbeatResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IConsumerPollingService;
import com.baracklee.mq.client.core.IMqGroupExecutorService;
import com.baracklee.mq.client.core.IMqHeartbeatService;
import com.baracklee.mq.client.core.IMqQueueExecutorService;
import com.baracklee.mq.client.dto.TraceMessageDto;
import com.baracklee.mq.client.resource.IMqResource;
import com.baracklee.mq.client.server.StatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqHeartbeatService implements IMqHeartbeatService {

    private Logger log = LoggerFactory.getLogger(MqHeartbeatService.class);

    private ScheduledExecutorService executor=null;

    private HeartbeatRequest request =null;

    private AtomicBoolean startFlag=new AtomicBoolean(false);

    private MqContext mqContext;

    private IMqResource mqResource;

    private volatile boolean isStop=false;

    private long count=0;

    public MqHeartbeatService() {
        this(MqClient.getMqFactory().createMqResource(MqClient.getContext().getConfig().getUrl(), 3500, 3500));
    }

    public MqHeartbeatService(IMqResource mqResource) {
        this.mqContext = MqClient.getContext();
        this.mqResource = mqResource;
        mqContext.setMqHtResource(this.mqResource);
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            isStop=false;
            executor = Executors.newScheduledThreadPool(1,
                    SoaThreadFactory.create("mq-HeartbeatService-pool-%d", Thread.MAX_PRIORITY - 1, true));
        }
        executor.scheduleAtFixedRate(()->{
            if(!isStop){
                doHeartbeat();
                checkMsgTimeOut();
                count++;
            }

        },1,10, TimeUnit.SECONDS);
        StatService.start();

    }

    private boolean checkTimeouting = false;

    private void checkMsgTimeOut() {
        if(checkTimeouting){return;}
        checkTimeouting=true;
        int warnTimeout = MqClient.getContext().getConfig().getWarnTimeout() * 1000;
        IConsumerPollingService consumerPollingService = MqClient.getMqFactory().createConsumerPollingService();
        Map<String, IMqGroupExecutorService> groups = consumerPollingService.getMqExecutors();

        for (Map.Entry<String, IMqGroupExecutorService> entry : groups.entrySet()) {
            List<TraceMessageDto> rs = new ArrayList<>(100);

            Map<Long, IMqQueueExecutorService> queueEx = entry.getValue().getQueueEx();
            for (Map.Entry<Long, IMqQueueExecutorService> serviceEntry : queueEx.entrySet()) {
                Collection<TraceMessageDto> values = serviceEntry.getValue().getSlowMsg().values();
                boolean flag=false;
                for (TraceMessageDto value : values) {
                    if (value.start>0&&System.currentTimeMillis()-value.start>warnTimeout){
                        rs.add(value);
                        flag=true;
                    }
                }
            }
            // if you want send mail plaese finish here
        }

    }

    private void doHeartbeat() {
        if(mqContext.getConsumerId()>0){
            if(request==null){
                request = new HeartbeatRequest();
            }
            request.setConsumerId(mqContext.getConsumerId());
            request.setAsyn(count % 3==0 ? 0:1);
            HeartbeatResponse response = mqResource.heartbeat(request);
            if(response!=null&&response.getDeleted()==1){
                MqClient.reStart();
                Util.sleep(5000);
            }
            if(response!=null){
                mqContext.setBakUrl(response.getBakUrl());
            }
        }
    }

    @Override
    public void close() {
        isStop=true;
        executor.shutdown();
        startFlag.set(false);
        request=null;
        executor=null;

    }
}
