package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IConsumerPollingService;
import com.baracklee.mq.client.core.IMqCommitService;
import com.baracklee.mq.client.core.IMqGroupExecutorService;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqCommitService implements IMqCommitService {

    private Logger log = LoggerFactory.getLogger(MqCommitService.class);

    private final MqContext mqContext;
    private final IMqResource mqResource;

    private volatile boolean isStop = false;
    private volatile boolean runStatus = false;
    private final AtomicBoolean startFlag = new AtomicBoolean(false);
    private ThreadPoolExecutor executor = null;

    public MqCommitService() {
        this(MqClient.getContext().getMqResource());
    }

    public MqCommitService(IMqResource mqResource) {
        this.mqContext = MqClient.getContext();
        this.mqResource = mqResource;
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("MqCommitService", true),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            isStop = false;
            runStatus = false;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while(!isStop){
                    runStatus=true;
                    commitData();
                    runStatus=false;
                    Util.sleep(mqContext.getConfig().getAynCommitInterval());
                }
            }
        });
        }
    }

    @Override
    public void close() {
        isStop=true;
        commitData();
        long start = System.currentTimeMillis();
        while (runStatus){
            Util.sleep(100);
            if (System.currentTimeMillis()-start>5000){
                break;
            }
        }

        if (executor!=null){
            executor.shutdown();
        }
        startFlag.set(false);
        executor=null;
    }

    private void commitData(){
        IConsumerPollingService consumerPollingService = MqClient.getMqFactory().createConsumerPollingService();
        Map<String, IMqGroupExecutorService> mqExecutors = consumerPollingService.getMqExecutors();
        CommitOffsetRequest commitOffsetRequest = new CommitOffsetRequest();
        List<ConsumerQueueVersionDto> queueVersionDtos = new ArrayList<>();
        commitOffsetRequest.setQueueOffsets(queueVersionDtos);
        mqExecutors.forEach((key, value) -> value.getQueueEx().forEach((key1, value1) -> {
            ConsumerQueueVersionDto changedCommit = value1.getChangedCommit();
            if (changedCommit != null) {
                queueVersionDtos.add(changedCommit);
            }
        }));
        if (!queueVersionDtos.isEmpty()){
            mqResource.commitOffset(commitOffsetRequest);
        }
    }

}
