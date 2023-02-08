package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.client.PullDataRequest;
import com.baracklee.mq.biz.dto.client.PullDataResponse;
import com.baracklee.mq.biz.event.IAsynSubscriber;
import com.baracklee.mq.biz.event.ISubscriber;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqQueueExecutorService;
import com.baracklee.mq.client.core.impl.queueutils.BatchRecorder;
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

public class MqQueueExecutorService implements IMqQueueExecutorService {

    public static final int COMMIT_TIME_DELTA = 20_000;
    private Logger log = LoggerFactory.getLogger(MqQueueExecutorService.class);
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

                //执行消费和提交偏移的线程
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        while (!isStop){
                            if(isRunning){
                                //此处不能加锁，会导致延迟消费阻塞
                                handleData();
                            }
                        }
                    }
                });
            }
        }
        }

    private void handleData() {
        ConsumerQueueDto temp=consumerQueueRef.get();
        runStatus=false;
        int msgSize=messages.size();
        refreshSubscriber();
        if (temp!=null&&msgSize>0 && 
                temp.getThreadSize()-taskCounter.get()>0&&
                (iSubscriber!=null||iAsynSubscriber!=null)&&
                (temp.getTimeout()>=0&&timeOutCount.get()==0)){
            //有个listener在这里,主要是用于检查是否外部要求暂停消费线程
            if(!checkPreHand(temp)) return;
            runStatus=true;
            doHandleData(temp,msgSize);
            runStatus=false;
        }else {
            Util.sleep(10);
        }
    }

    private void doHandleData(ConsumerQueueDto temp, int msgSize) {
        int threadSize=temp.getThreadSize()-taskCounter.get();
        int startThread=(msgSize+temp.getConsumerBatchSize()-1)/temp.getConsumerBatchSize();
        if(startThread>=threadSize){
            startThread=threadSize;
        }
        if(startThread>temp.getThreadSize()){
            startThread=temp.getThreadSize();
        }
        if(batchRecorder.)
    }

    private long lastRefresh=System.currentTimeMillis();
    private long lastSend=System.currentTimeMillis()-37_000;
    private void refreshSubscriber() {
        try {
            if (mqContext.getMqEvent().getiSubscriberSelector() != null
                    || mqContext.getMqEvent().getiAsynSubscriberSelector() != null) {
                if (System.currentTimeMillis() - lastRefresh > 5000) {
                    if (mqContext.getMqEvent().getiSubscriberSelector() != null) {
                        iSubscriber = mqContext.getMqEvent().getiSubscriberSelector().getSubscriber(consumerGroupName,
                                consumerQueueRef.get().getOriginTopicName());
                    }
                    if (iSubscriber == null && mqContext.getMqEvent().getiAsynSubscriberSelector() != null) {
                        iAsynSubscriber = mqContext.getMqEvent().getiAsynSubscriberSelector()
                                .getSubscriber(consumerGroupName, consumerQueueRef.get().getOriginTopicName());
                    }
                    if (iSubscriber == null && iAsynSubscriber == null
                            && System.currentTimeMillis() - lastSend > 360000) {
                        log.error(consumerQueueRef.get()+ "此消费者组下" + consumerQueueRef.get().getTopicName() + "没有消费处理类！");
                        lastSend = System.currentTimeMillis();
                    }
                    lastRefresh = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            log.error("getSubscriber_error", e);
        }
    }

    private boolean checkPreHand(ConsumerQueueDto temp) {
        if(MqClient.getContext().getMqEvent().getPreHandleListener()==null) return false;
        return (MqClient.getContext().getMqEvent().getPreHandleListener().preHandle(temp));
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
                        log.warn("拉去的消息信息{}", JsonUtil.toJson(response));
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

    protected void cacheData(PullDataResponse response, ConsumerQueueDto consumerQueueDto) {
        if(checkOffsetVersion(consumerQueueDto)){
            for (MessageDto msg : response.getMsgs()) {
                if(!checkOffsetVersion(consumerQueueDto)){
                    messages.clear();
                    break;
                }
                while (checkOffsetVersion(consumerQueueDto)){
                    TraceMessageDto traceMessageDto = new TraceMessageDto(msg, consumerQueueDto);
                    if(slowMsgMap.size()>350){
                        log.error("slowMsg is {}",JsonUtil.toJsonNull(slowMsgMap));
                        slowMsgMap.clear();
                    }
                    try {
                        messages.put(traceMessageDto);
                    } catch (InterruptedException ignore) {
                    }
                    Util.sleep(100);
                }
                updateLastId(consumerQueueDto,msg);
            }
        }
    }

    private void updateLastId(ConsumerQueueDto consumerQueueDto, MessageDto msg) {
        synchronized (lockMetaObj){
            if (lastId<msg.getId()&&checkOffsetVersion(consumerQueueDto)){
                lastId= msg.getId();
                consumerQueueDto.setLastId(lastId);
            }
        }
    }

    public boolean checkOffsetVersion(ConsumerQueueDto consumerQueueDto) {
        return consumerQueueDto.getOffsetVersion()==consumerQueueRef.get().getOffsetVersion();
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
