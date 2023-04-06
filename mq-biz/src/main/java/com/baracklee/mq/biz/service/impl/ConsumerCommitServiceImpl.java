package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.biz.dto.client.CommitOffsetResponse;
import com.baracklee.mq.biz.entity.OffsetVersionEntity;
import com.baracklee.mq.biz.service.ConsumerCommitService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author:  BarackLee
 */
@Service
public class ConsumerCommitServiceImpl implements ConsumerCommitService, BrokerTimerService {

    private static final Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SoaConfig soaConfig;

    private QueueOffsetService queueOffsetService;

    private ConsumerGroupService consumerGroupService;

    private volatile boolean isRunning = true;
    private ThreadPoolExecutor executorRun = null;
    private volatile int commitThreadSize = 5;
    private volatile int commitUpdateThreadSize = 20;
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private ThreadPoolExecutor executorCommit = null;

    protected final AtomicReference<Map<Long, ConsumerQueueVersionDto>> mapAppPolling = new AtomicReference<>(
            new ConcurrentHashMap<>(4000));
    protected final Map<Long, ConsumerQueueVersionDto> failMapAppPolling = new ConcurrentHashMap<>(100);

    @Autowired
    public ConsumerCommitServiceImpl(SoaConfig soaConfig, QueueOffsetService queueOffsetService, ConsumerGroupService consumerGroupService) {
        this.soaConfig = soaConfig;
        this.queueOffsetService = queueOffsetService;
        this.consumerGroupService = consumerGroupService;
    }

    @Override
    public CommitOffsetResponse commitOffset(CommitOffsetRequest request) {
        return null;
    }

    @Override
    public Map<Long, ConsumerQueueVersionDto> getCache() {
        return mapAppPolling.get();
    }

    @Override
    public void startBroker() {
        commitThreadSize=soaConfig.getCommitThreadSize();
        executorRun = new ThreadPoolExecutor(commitThreadSize + 1, commitThreadSize + 1, 10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200), SoaThreadFactory.create("commit-run", Thread.MAX_PRIORITY - 1, true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        executorCommit = new ThreadPoolExecutor(2, commitUpdateThreadSize, 10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(500), SoaThreadFactory.create("commit-update", Thread.MAX_PRIORITY - 1, true),
                new ThreadPoolExecutor.CallerRunsPolicy());

        soaConfig.registerChanged(new Runnable() {
            @Override
            public void run() {
                if(commitThreadSize!=soaConfig.getCommitThreadSize()){
                    commitThreadSize=soaConfig.getCommitThreadSize();
                    executorRun.setCorePoolSize(commitThreadSize+1);
                    executorRun.setMaximumPoolSize(commitThreadSize+1);
                }

                if (commitUpdateThreadSize != soaConfig.getCommitUpdateThreadSize()) {
                    commitUpdateThreadSize = soaConfig.getCommitUpdateThreadSize();
                    executorCommit.setMaximumPoolSize(commitUpdateThreadSize);
                }
            }
        });

        executorRun.execute(()->{
            commitOffset();
        });
    }

    protected void commitOffset() {
        log.info("doSubmitOffset");
        while (isRunning) {
            doCommit();
            //减少服务器的压力
            Util.sleep(soaConfig.getCommitSleepTime());
        }
    }

    private void doCommit() {
        Map<Long, OffsetVersionEntity> offsetVersionMap = queueOffsetService.getOffsetVersion();
        Map<Long, ConsumerQueueVersionDto> map = new HashMap<>(mapAppPolling.get());
        if(map.size()>0){
            final int size = map.size();

            int countSize=map.size()<commitThreadSize?map.size():commitThreadSize;

            if (countSize==1){
                for (Map.Entry<Long, ConsumerQueueVersionDto> entry : map.entrySet()) {
                    doCommitOffset(entry.getValue(), 0, offsetVersionMap, size);
                }
            }else {
                CountDownLatch count= new CountDownLatch(countSize);
                for (Map.Entry<Long, ConsumerQueueVersionDto> entry : map.entrySet()) {
                    executorRun.execute(new Runnable() {
                        @Override
                        public void run() {
                            doCommitOffset(entry.getValue(),0,offsetVersionMap,size);
                            count.countDown();
                        }
                    });
                }
                try {
                    count.await();
                } catch (InterruptedException e) {
                    log.error("commit thread error",e);
                }finally {
                    count.countDown();
                }
            }
        }
    }

    private void doCommitOffset(ConsumerQueueVersionDto request,
                                int flag,
                                Map<Long, OffsetVersionEntity> offsetVersionMap,
                                int count) {
        OffsetVersionEntity offsetVersionEntity = offsetVersionMap.get(request.getQueueOffsetId());
    }


    @Override
    public void stopBroker() {

        try {
            isRunning = false;
            executorRun.shutdown();
            executorCommit.shutdown();
        }catch (Throwable e){
            log.error("commitService_error",e);
        }

    }

    @Override
    public String info() {
        return null;
    }
}
