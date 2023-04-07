package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.biz.dto.client.CommitOffsetResponse;
import com.baracklee.mq.biz.entity.OffsetVersionEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.ConsumerCommitService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.invoke.MethodHandles;
import java.util.*;
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

    protected long lastTime = System.currentTimeMillis();

    private Object lockObj = new Object();
    private Object lockObj1 = new Object();
    /**
     * 添加request 进入抽象数组，待会查询
     * @param request
     * @return
     */
    @Override
    public CommitOffsetResponse commitOffset(CommitOffsetRequest request) {

        CommitOffsetResponse response = new CommitOffsetResponse();
        response.setSuc(true);
        Map<Long, ConsumerQueueVersionDto> map = mapAppPolling.get();
        try {
            if (request != null && !CollectionUtils.isEmpty(request.getQueueOffsets())) {
                request.getQueueOffsets().forEach(t1 -> {
                    ConsumerQueueVersionDto temp = map.get(t1.getQueueOffsetId());
                    boolean flag1 = true;
                    if (temp == null) {
                        synchronized (lockObj1) {
                            temp = map.get(t1.getQueueOffsetId());
                            if (temp == null) {
                                map.put(t1.getQueueOffsetId(), t1);
                                flag1 = false;
                            }
                        }
                    }
                    if (flag1) {
                        if (temp.getOffsetVersion() < t1.getOffsetVersion()) {
                            clearOldData();
                            map.put(t1.getQueueOffsetId(), t1);
                        } else if (temp.getOffsetVersion() == t1.getOffsetVersion()
                                && temp.getOffset() < t1.getOffset()) {
                            clearOldData();
                            map.put(t1.getQueueOffsetId(), t1);
                        }
                    }
                });
                if (request.getFlag() == 1) {
                    executorCommit.submit(new Runnable() {
                        @Override
                        public void run() {
                            commitAndUpdate(request);
                        }
                    });
                }
            }
        } catch (Exception e) {
        }
        // catTransaction.setStatus(Transaction.SUCCESS);
        // catTransaction.complete();
        return response;
    }

    private void commitAndUpdate(CommitOffsetRequest request) {
        Set<String> consumerGroupNames = new HashSet<>();
        Map<Long, OffsetVersionEntity> versionEntityMap = queueOffsetService.getOffsetVersion();
        for (ConsumerQueueVersionDto queueOffset : request.getQueueOffsets()) {
            doCommitOffset(queueOffset,1,versionEntityMap,0);
            consumerGroupNames.add(queueOffset.getConsumerGroupName())
        }
        if (consumerGroupNames.size()>0){
            consumerGroupService.notifyMetaByNames(new ArrayList<>(consumerGroupNames));
        }
    }

    private void clearOldData() {
        boolean flag = (System.currentTimeMillis() - lastTime - 10 * 60 * 1000) > 0;
        if (flag) {
            synchronized (lockObj) {
                if ((System.currentTimeMillis() - lastTime - 30 * 60 * 1000) > 0) {
                    mapAppPolling.set(new ConcurrentHashMap<>(failMapAppPolling));
                    failMapAppPolling.clear();
                    lastTime = System.currentTimeMillis();
                }
            }
        }
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

    private boolean doCommitOffset(ConsumerQueueVersionDto request,
                                int flag,
                                Map<Long, OffsetVersionEntity> offsetVersionMap,
                                int count) {
        try {
            OffsetVersionEntity offsetVersionEntity = offsetVersionMap.get(request.getQueueOffsetId());
            if(checkOffsetAndVersion(request,offsetVersionEntity)){
                QueueOffsetEntity queueOffsetEntity = new QueueOffsetEntity();
                queueOffsetEntity.setId(request.getQueueOffsetId());
                queueOffsetEntity.setOffsetVersion(request.getOffsetVersion());
                queueOffsetEntity.setOffset(request.getOffset());
                queueOffsetEntity.setConsumerGroupName(request.getConsumerGroupName());
                queueOffsetEntity.setTopicName(request.getTopicName());
                boolean rs = false;
                if (flag == 1) {
                    rs = queueOffsetService.commitOffsetAndUpdateVersion(queueOffsetEntity) > 0 && offsetVersionEntity != null;
                    if(rs){
                        queueOffsetEntity.setOffsetVersion(queueOffsetEntity.getOffsetVersion()+1);
                    }
                } else {
                    rs = queueOffsetService.commitOffset(queueOffsetEntity) > 0 && offsetVersionEntity != null;
                }
                if (rs) {
                    reentrantLock.lock();
                    if (request.getOffsetVersion() == offsetVersionEntity.getOffsetVersion()
                            && request.getOffset() > offsetVersionEntity.getOffset()) {
                        offsetVersionEntity.setOffset(request.getOffset());
                    } else if (request.getOffsetVersion() > offsetVersionEntity.getOffsetVersion()) {
                        offsetVersionEntity.setOffsetVersion(request.getOffsetVersion());
                        offsetVersionEntity.setOffset(request.getOffset());
                    }
                    reentrantLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("commit_offset_error",e);
            return false;
        }
        return true;
    }

    protected boolean checkOffsetAndVersion(ConsumerQueueVersionDto request, OffsetVersionEntity offsetVersionEntity) {
        if (offsetVersionEntity == null) {
            return true;
        } else if (request.getOffsetVersion() > offsetVersionEntity.getOffsetVersion()) {
            return true;
        } else if (request.getOffset() > offsetVersionEntity.getOffset()) {
            return true;
        }
        return false;
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
