package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.GetMetaGroupRequest;
import com.baracklee.mq.biz.dto.client.GetMetaGroupResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqBrokerUrlRefreshService;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqBrokerUrlRefreshService implements IMqBrokerUrlRefreshService {
    private Logger log = LoggerFactory.getLogger(MqBrokerUrlRefreshService.class);
    private ScheduledExecutorService executor = null;
    private GetMetaGroupRequest request = new GetMetaGroupRequest();
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private MqContext mqContext;
    private IMqResource mqResource;
    private volatile boolean isStop = false;
    private volatile boolean runStatus = false;

    public MqBrokerUrlRefreshService(){
        this(MqClient.getContext().getMqResource());
    }

    public MqBrokerUrlRefreshService(IMqResource mqResource) {
        this.mqContext=MqClient.getContext();
        this.mqResource=mqResource;
    }


    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            isStop=false;
            runStatus=false;
            doUpdateBrokerUrls();
            executor = Executors.newScheduledThreadPool(1,
                    SoaThreadFactory.create("mq-brokerFreshService-pool-%d", Thread.MAX_PRIORITY - 1, true));        }
        executor.scheduleAtFixedRate((()->{
            if(!isStop){
                runStatus=true;
                doUpdateBrokerUrls();
                runStatus=false;
            }
        }),1,20, TimeUnit.SECONDS);

    }

    private void doUpdateBrokerUrls() {
        GetMetaGroupResponse response = this.mqResource.getMetaGroup(request);
        if(response==null) return;
        if(response.isSuc()){
            mqContext.setBrokerMetaMode(response.getBrokerMetaMode());
            mqContext.setMetricUrl(response.getMetricUrl());
            if (mqContext.getMetricUrl().isEmpty()){
                MqClient.getMqFactory().createMqMeticReporterService().close();
            }else{
                MqClient.getMqFactory().createMqMeticReporterService().start();
            }
        }
        if(mqContext.getBrokerMetaMode()==1||mqContext.getBrokerMetaMode()==0&&mqContext.getConfig().isMetaMode()){
            if(response.getBrokerIpG1()!=null){
                mqContext.setBrokerUrls(response.getBrokerIpG1(),response.getBrokerIpG2());
            }
        }else if(mqContext.getBrokerMetaMode()==-1||mqContext.getConfig().isMetaMode()){
            mqContext.setBrokerUrls(new ArrayList<>(),new ArrayList<>());
        }
    }

    @Override
    public void close() {
        isStop=true;
        long start=System.currentTimeMillis();
        while (runStatus){
            Util.sleep(10);
            if(System.currentTimeMillis()-start>5000){
                break;
            }
        }
        executor.shutdown();
        startFlag.set(false);
        executor=null;
    }
}
