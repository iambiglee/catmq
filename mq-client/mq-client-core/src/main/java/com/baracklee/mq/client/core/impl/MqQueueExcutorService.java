package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.client.PullDataRequest;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqQueueExecutorService;
import com.baracklee.mq.client.dto.TraceMessageDto;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MqQueueExcutorService implements IMqQueueExecutorService {

    public static final int COMMIT_TIME_DELTA = 20_000;
    private Logger log = LoggerFactory.getLogger(MqQueueExcutorService.class);
    private AtomicReference<ConsumerQueueDto> consumerQueueRef = new AtomicReference<ConsumerQueueDto>();
    private ThreadPoolExecutor executor=null;
    private volatile ThreadPoolExecutor executorNotify = null;
    private String consumerGroupName;
    private volatile boolean isRunning = false;
    private volatile long lastId = 0;
    private BlockingQueue<TraceMessageDto> messages = new ArrayBlockingQueue<>(300);
    private Map<Long, TraceMessageDto> slowMsgMap = new ConcurrentHashMap<>(350);
    private PullDataRequest request = new PullDataRequest();
    private ISubscriber iSubscriber = null;
    private IAsynSubscriber iAsynSubscriber = null;
    private AtomicInteger failCount = new AtomicInteger(0);// 处理失败次数
    private volatile long failBeginTime = 0;// 处理失败开始时间
    private TraceMessage traceMsgPull = null;
    private TraceMessage traceMsgDeal = null;
    private TraceMessage traceMsgCommit = null;
    private TraceMessage traceMsg = null;
    private MqContext mqContext;
    private IMqResource mqResource;
    private volatile boolean isStop = false;
    private AtomicBoolean isStart = new AtomicBoolean(false);
    private volatile boolean runStatus = false;
    // private final Object lockOffsetObj = new Object();
    private final Object lockMetaObj = new Object();

    private BatchRecorder batchRecorder = new BatchRecorder();
    // public volatile boolean timeOutFlag = true;
    private AtomicInteger timeOutCount = new AtomicInteger(0);
    private AtomicInteger taskCounter = new AtomicInteger(0);


    @Override
    public void start() {
        if (this.iSubscriber != null || this.iAsynSubscriber != null) {
            if(isStart.compareAndSet(false,true)){
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            pullingData();
                        }catch ( Exception ignore){}
                    }
                });

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        while (!isStop){
                            if(isRunning){
                                //此处不能加锁，会导致延迟消费阻塞
                                handleDate();
                            }
                        }
                    }
                });
            }
        }
        }

    private void pullingData() {
        boolean flag= false;
        int sleepTime=500;
        while (!isStop){
            if(isRunning){
                flag=doPullingData();
            }
            if (flag){
                sleepTime=0;
            }else {
                sleepTime=sleepTime+mqContext.getConfig().getPullDeltaTime();
                if(sleepTime>=consumerQueueRef.get().getMaxPullTime()*1000) sleepTime=50;
            }
            if (sleepTime>0){
                Util.sleep(sleepTime);
            }
        }
    }

    private volatile long lastPullTime = System.currentTimeMillis();
    private AtomicBoolean pullFlag = new AtomicBoolean(false);
    private boolean doPullingData() {
        if (pullFlag.compareAndSet(false,true)){
            lastPullTime=System.currentTimeMillis();
            ConsumerQueueDto consumerQueueDto = consumerQueueRef.get();
            if(consumerQueueDto!=null){

                try {
                    request.setQueueId(consumerQueueDto.getQueueId());
                    if(checkOffsetVersion(consumerQueueDto)){
                        consumerQueueDto.setLastId(lastId);
                        request.setOffsetStart(lastId);
                        request.setConsumerGroupName(consumerQueueDto.getConsumerGroupName());
                        request.setTopicName(consumerQueueDto.getTopicName());
                        PullDataResponse response = null;
                        if(checkOffsetVersion(consumerQueueDto)){
                            response=mqResource.pullData(request);
                        }
                        log.warn("拉去消息lastid:{}",lastId);
                        log.warn("拉去的消息信息{}",response.toString);
                        if (response != null && response.getMsgs() != null && response.getMsgs().size() > 0) {
                            cacheData(response, consumerQueueDto);
                            return true;
                        }
                    }
                } finally {
                    pullFlag.set(false);
                }
            }
            return false;
        }else {
            return true;
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void updateQueueMeta(ConsumerQueueDto consumerQueue) {

    }

    @Override
    public void notifyMsg() {

    }

    @Override
    public void commit(List<MessageDto> failMsgs, ConsumerQueueDto consumerQueue) {

    }

    @Override
    public Map<Long, TraceMessageDto> getSlowMsg() {
        return null;
    }

    @Override
    public ConsumerQueueVersionDto getChangedCommit() {
        return null;
    }

    @Override
    public ConsumerQueueVersionDto getLast() {
        return null;
    }

    @Override
    public boolean hasFininshed() {
        return false;
    }

    @Override
    public void stop() {

    }
}
