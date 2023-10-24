package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.trace.TraceMessageItem;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.event.IAsynSubscriber;
import com.baracklee.mq.biz.event.IMsgFilter;
import com.baracklee.mq.biz.event.ISubscriber;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqQueueExecutorService;
import com.baracklee.mq.client.core.impl.queueutils.BatchRecorder;
import com.baracklee.mq.client.core.impl.queueutils.MessageInvokeCommandForThreadIsolation;
import com.baracklee.mq.client.dto.BatchRecordItem;
import com.baracklee.mq.client.dto.TraceMessageDto;
import com.baracklee.mq.client.resource.IMqResource;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

    private Object lockObject = new Object();

    public MqQueueExecutorService(String consumerGroupName, ConsumerQueueDto consumerQueue) {
        this.mqContext= MqClient.getContext();
        this.mqResource=mqContext.getMqResource();
        this.consumerGroupName=consumerGroupName;
        initTraceAndSubscriber(consumerGroupName,consumerQueue);
        consumerQueueRef.set(consumerQueue);
        createExecutor(consumerQueue);
        this.lastId=consumerQueue.getOffset();
        consumerQueue.setLastId(lastId);
        isRunning=consumerQueueRef.get().getStopFlag()==0;
    }


    private void createExecutor(ConsumerQueueDto consumerQueue) {
        if(executor==null||executor.isShutdown()||executor.isTerminated()){
            executor = new ThreadPoolExecutor(consumerQueue.getThreadSize() + 2, consumerQueue.getThreadSize() + 2, 0L,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100),
                    SoaThreadFactory.create(
                            "MqQueueExcutorServiceNew-" + consumerGroupName + "-" + consumerQueue.getQueueId(), true),
                    new ThreadPoolExecutor.DiscardOldestPolicy());        }
        log.info("创建queue{}_{}",consumerQueue.getQueueId(),consumerQueue.getQueueOffsetId());
    }

    private void initTraceAndSubscriber(String consumerGroupName, ConsumerQueueDto consumerQueue) {
        this.iSubscriber = mqContext.getSubscriber(consumerGroupName, consumerQueue.getOriginTopicName());
        if(this.iSubscriber==null){
            this.iAsynSubscriber = mqContext.getAsynSubscriber(consumerGroupName, consumerQueue.getOriginTopicName());
            if (this.iAsynSubscriber==null){
                log.warn("consumerGroup_{}_{},没有订阅接口",consumerQueue.getTopicName(),consumerQueue.getOriginTopicName());
            }
        }
    }


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
        if(batchRecorder.getRecordMap().size()>200){
            log.error("消息堆积:{}",slowMsgMap.size());
        }
        long batchRecordId=batchRecorder.begin(startThread);
        CountDownLatch countDownLatch = new CountDownLatch(startThread);
        batchExecute(temp,msgSize,startThread,batchRecordId,countDownLatch);
        try {
            countDownLatch.await();
        } catch (InterruptedException ignore) {

        }finally {
            countDownLatch.countDown();
        }
    }

    private void batchExecute(ConsumerQueueDto temp, int msgSize, int startThread, long batchRecordId, CountDownLatch countDownLatch) {
        for (int i = 0; i < startThread; i++) {
            if(executor!=null)
                executor.execute(new MsgThread(temp,batchRecordId,countDownLatch,timeOutCount));
        }
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
                        request.setOffsetEnd(lastId + consumerQueueDto.getPullBatchSize());
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
                        break;
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
        stop();
        messages.clear();
        if(executor!=null){
            executor.shutdown();
            executor=null;
        }
        slowMsgMap.clear();
        isStart.set(false);
    }

    @Override
    public void updateQueueMeta(ConsumerQueueDto consumerQueue) {
        synchronized (lockMetaObj){
            doUpdateQueueMeta(consumerQueue);
        }
    }

    private void doUpdateQueueMeta(ConsumerQueueDto consumerQueue) {
        if(consumerQueue.getTimeout()==0){
            timeOutCount.set(0);
        }
        ConsumerQueueDto dto = consumerQueueRef.get();
        boolean flag = consumerQueue.getThreadSize() != dto.getThreadSize();
        if(flag){
            log.info("更新线程数量{}",consumerQueue.getTopicName());
            executor.setCorePoolSize(consumerQueue.getThreadSize()+2);
            executor.setMaximumPoolSize(consumerQueue.getThreadSize()+2);
        }
        //更新元数据
        if(consumerQueue.getOffsetVersion()==dto.getOffsetVersion()){
            log.info("update meta with topic {}",consumerQueue.getTopicName());
            updateQueueMetaWithOutoffset(consumerQueue);
        }else {
            //重平衡的偏移也记录在这里
            log.info("queue offset change, 发生队列重平衡{}",consumerQueue.getTopicName());
            consumerQueueRef.set(consumerQueue);
            //给消息处理多一点时间
            Util.sleep(100);
            //为了防止拉倒数据,阻塞当前线程处理
            messages.clear();

            slowMsgMap.clear();
            //重新获取偏移
            consumerQueue.setLastId(consumerQueue.getOffset());
            this.lastId=consumerQueue.getOffset();
        }
        if(isRunning&&consumerQueue.getStopFlag()==1){
            log.info("stop deal,停止消费" + consumerQueue.getTopicName());
            isRunning=consumerQueue.getStopFlag()==0;
            doCommit(dto);
        }
        isRunning=consumerQueue.getStopFlag()==0;

    }

    private void updateQueueMetaWithOutoffset(ConsumerQueueDto consumerQueue) {
        resetConsumerBatchSize(consumerQueue);
        consumerQueueRef.get().setConsumerBatchSize(consumerQueue.getConsumerBatchSize());
        consumerQueueRef.get().setDelayProcessTime(consumerQueue.getDelayProcessTime());
        consumerQueueRef.get().setPullBatchSize(consumerQueue.getPullBatchSize());
        consumerQueueRef.get().setRetryCount(consumerQueue.getRetryCount());
        consumerQueueRef.get().setStopFlag(consumerQueue.getStopFlag());
        consumerQueueRef.get().setTag(consumerQueue.getTag());
        consumerQueueRef.get().setThreadSize(consumerQueue.getThreadSize());
        consumerQueueRef.get().setTraceFlag(consumerQueue.getTraceFlag());
        consumerQueueRef.get().setMaxPullTime(consumerQueue.getMaxPullTime());
        consumerQueueRef.get().setTimeout(consumerQueue.getTimeout());
    }

    private void resetConsumerBatchSize(ConsumerQueueDto consumerQueue) {
        if (consumerQueue.getConsumerBatchSize() < 0) {
            consumerQueue.setConsumerBatchSize(1);
            log.warn("ConsumerBatchSize error!");
        }
    }

    private void doCommit(ConsumerQueueDto temp) {
        BatchRecordItem item = batchRecorder.getLastestItem();
        if (item != null) {
            doCommit(temp, item, true);
        }
    }
    @Override
    public void notifyMsg() {
        createExecutorNotify();
        if(System.currentTimeMillis()>lastPullTime){
            executorNotify.submit(new Runnable() {
                @Override
                public void run() {
                    doPullingData();
                }
            });
        }
    }

    private void createExecutorNotify() {
        if(executorNotify==null){
            synchronized (this){
                if (executorNotify==null){
                    if(executorNotify==null||executorNotify.isTerminated()||executorNotify.isShutdown()){
                        executorNotify = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>(100),
                                SoaThreadFactory.create("MqQueueExcutorService-notify-" + consumerGroupName + "-"
                                        + consumerQueueRef.get().getQueueId(), true),
                                new ThreadPoolExecutor.DiscardOldestPolicy());
                    }
                }
            }
        }
    }

    @Override
    public void commit(List<MessageDto> failMsgs, ConsumerQueueDto consumerQueue) {
        if(checkOffsetVersion(consumerQueue)){
            if(failMsgs!=null&&failMsgs.isEmpty()){
                Map<Long, MessageDto> failMsg = new HashMap<>(failMsgs.size());
                failAlarm(failMsg,consumerQueue);
                PublishMessageRequest failMsgRequest = getFailMsgRequest(consumerQueue, new ArrayList<>(failMsg.values()));
                publishAndUpdateResultFailMsg(failMsgRequest,consumerQueue,null);
            }
            if(checkOffsetVersion(consumerQueue)&&consumerQueue.getOffset()>consumerQueueVersionDto.getOffset()){
                consumerQueueVersionDto.setOffset(consumerQueue.getOffset());
                consumerQueueVersionDto.setOffsetVersion(consumerQueue.getOffsetVersion());
                consumerQueueVersionDto.setQueueOffsetId(consumerQueue.getQueueOffsetId());
                consumerQueueVersionDto.setConsumerGroupName(consumerQueue.getConsumerGroupName());
                consumerQueueVersionDto.setTopicName(consumerQueue.getTopicName());
                consumerQueueRef.get().setOffset(consumerQueue.getOffset());
            }
        }
    }

    @Override
    public Map<Long, TraceMessageDto> getSlowMsg() {
        return slowMsgMap;
    }

    @Override
    public ConsumerQueueVersionDto getChangedCommit() {
        long commitVersion = this.commitVersion.get();
        if(hasCommitVersion<commitVersion){
            hasCommitVersion=commitVersion;
            return consumerQueueVersionDto;
        }
        if(hasCommitVersion>0&&System.currentTimeMillis()-lastCommitTime>COMMIT_TIME_DELTA){
            return consumerQueueVersionDto;
        }
        return null;
    }

    @Override
    public ConsumerQueueVersionDto getLast() {
        if(hasCommitVersion>0){
            return consumerQueueVersionDto;
        }
        return null;
    }

    @Override
    public boolean hasFininshed() {
        return taskCounter.get()==0;
    }

    @Override
    public void stop() {
        isRunning=false;
        isStop=true;

    }

    public class Pair<T1, T2> {
        public T1 item1;
        public T2 item2;
    }

    protected long threadExcute(ConsumerQueueDto pre,CountDownLatch countDownLatch){
        if(isRunning&&(iSubscriber!=null||iAsynSubscriber!=null)){
            Map<Long, MessageDto> messageMap = new LinkedHashMap<>();
            Pair<Long, Boolean> pair = prepareValue(pre, messageMap);
            countDownLatch.countDown();
            long maxId=pair.item1;
            boolean flag= pair.item2;
            if(messageMap.size()>0){
                List<Long> failIds = invokeMessage(pre, messageMap);
                List<Long> sucIds = new ArrayList<>();
                Map<Long,MessageDto> failMsg=getFailMsg(pre,failIds,sucIds,messageMap);
                failAlarm(failMsg,pre);
                //发送失败队列消息到服务端
                PublishMessageRequest failRequest = getFailMsgRequest(pre, new ArrayList<>(failMsg.values()));
                publishAndUpdateResultFailMsg(failRequest, pre, sucIds);
                return maxId;
            }else {
                if (flag){
                    return maxId;
                }
            }
        }else {
            countDownLatch.countDown();
        }
        return 0;
    }
    protected void publishAndUpdateResultFailMsg(PublishMessageRequest failRequest, ConsumerQueueDto pre,
                                                 List<Long> sucIds) {
        FailMsgPublishAndUpdateResultRequest request = new FailMsgPublishAndUpdateResultRequest();
        if (sucIds != null && sucIds.size() > 0) {
            request.setIds(sucIds);
        }
        request.setQueueId(pre.getQueueId());
        if (failRequest != null) {
            request.setFailMsg(failRequest);
        }
        if ((sucIds != null && sucIds.size() > 0) || failRequest != null) {
            mqResource.publishAndUpdateResultFailMsg(request);
        }
    }

    //失败消息重试,或者加入失败消息队列
    private PublishMessageRequest getFailMsgRequest(ConsumerQueueDto pre, ArrayList<MessageDto> messageDtos) {
        String failTopicName= String.format("%s_%s_fail", consumerGroupName, pre.getOriginTopicName());
        List<MessageDto> messageDtos1 = new ArrayList<>(messageDtos.size());
        messageDtos.forEach(messageDto->{
            messageDto.setRetryCount(messageDto.getRetryCount()+1);
            if(pre.getRetryCount()>=messageDto.getRetryCount()){
                messageDtos1.add(messageDto);
            }else {
                log.error("当前消息达到了最大重试次数{},此条消息会丢失{}",pre.getRetryCount(),messageDto);
            }
        });
        if(messageDtos1.size()>0) {
            PublishMessageRequest request = new PublishMessageRequest();
            request.setTopicName(failTopicName);
            List<ProducerDataDto> msgsList=new ArrayList<>();
            messageDtos1.forEach(t->{
                ProducerDataDto dto=new ProducerDataDto();
                BeanUtils.copyProperties(dto,t);
                msgsList.add(dto);
            });
            request.setMsgs(msgsList);
            if (!Util.isEmpty(mqContext.getConfig().getIp())) {
                request.setClientIp(mqContext.getConfig().getIp());
            }
            return request;
        }
        return new PublishMessageRequest();
    }

    private void failAlarm(Map<Long, MessageDto> failMsg, ConsumerQueueDto pre) {
        if(failMsg.size()>0){
            failMsg.values().forEach(t1->{
                failCount.incrementAndGet();
            });
        }else {
            failCount.set(0);
            failBeginTime=0L;
        }
    }

    private Map<Long, MessageDto> getFailMsg(ConsumerQueueDto pre, List<Long> failIds, List<Long> sucIds, Map<Long, MessageDto> messageMap) {
        Map<Long,MessageDto> messageMap1=new HashMap<>();
        failIds.forEach(t1->{
            messageMap1.put(t1,messageMap1.get(t1));
        });
        if(pre.getTopicType()==2){
            messageMap.forEach((key, value) -> {
                if (!messageMap1.containsKey(key)) {
                    sucIds.add(key);
                }
            });
        }
        return messageMap1;
    }

    private List<Long> invokeMessage(ConsumerQueueDto pre, Map<Long, MessageDto> messageMap) {
        List<Long> failIds = null;

        try {
            List<MessageDto> dtos = new ArrayList<>(messageMap.values());
            failIds = doMessageReceived(dtos);
        } finally {
            if(failIds==null) failIds=new ArrayList<>();
            messageMap.forEach((key, value) -> slowMsgMap.remove(key));
        }
        if (MqClient.getContext().getMqEvent().getPostHandleListener()!=null) {
            MqClient.getContext().getMqEvent().getPostHandleListener().postHandle(pre,failIds.isEmpty());
        }
        return failIds;
    }

    private List<Long> doMessageReceived(List<MessageDto> dtos) {
        return MessageInvokeCommandForThreadIsolation.invoke(dtos, iSubscriber, iAsynSubscriber,
                consumerQueueRef.get());
    }

    private Pair<Long, Boolean> prepareValue(ConsumerQueueDto pre, Map<Long, MessageDto> messageMap) {
        Pair<Long, Boolean> pair = new Pair<>();
        long maxId = 0;
        // boolean flag = false;
        pair.item1 = 0L;
        pair.item2 = false;
        int count=0;
        while (count<pre.getConsumerBatchSize()){
            TraceMessageDto traceMessageDto = messages.poll();
            if (isRunning&&traceMessageDto!=null&&checkOffsetVersion(pre)) {
                slowMsgMap.put(traceMessageDto.getId(), traceMessageDto);
                MessageDto dto = traceMessageDto.message;
                if(onMsgFilter(dto)&&checkDealy(dto,pre)&&checkRetryCount(dto,pre)){
                    dto.setTopicName(pre.getOriginTopicName());
                    dto.setConsumerGroupName(pre.getConsumerGroupName());
                    messageMap.put(dto.getId(),dto);
                }
                maxId=maxId<dto.getId()?dto.getId():maxId;
                pair.item1=maxId;
                pair.item2=true;
            }
            count++;
        }
    return pair;
    }

    private boolean checkRetryCount(MessageDto dto, ConsumerQueueDto pre) {
        return pre.getTopicType()==1||(pre.getTopicType()==2||pre.getRetryCount()>dto.getRetryCount());
    }

    // 检查是否需要延迟执行，注意需要保证服务器时间与消费机器本地时间不一致的问题,为了减轻服务器数据库的压力，需要假定数据库时间是同步的
    protected boolean checkDealy(MessageDto messageDto, ConsumerQueueDto temp) {
        if (temp.getDelayProcessTime() > 0) {
            long delta = messageDto.getSendTime().getTime() + temp.getDelayProcessTime() * 1000
                    - System.currentTimeMillis();
            if (delta > 0) {
                Util.sleep((int) delta);
                log.info("topic:" + temp.getTopicName() + "延迟" + delta + "毫秒");
            }
        }
        return true;
    }
    private boolean onMsgFilter(MessageDto messageDto) {
        List<IMsgFilter> msgFilters = mqContext.getMqEvent().getMsgFilters();
        boolean flag = true;
        for (IMsgFilter msgFilter : msgFilters) {
            try {
                flag = msgFilter.onMsgFilter(messageDto);
                if (!flag) {
                    return false;
                }
            } catch (Exception e) {

            }
        }
        return true;
    }
    public class MsgThread implements Runnable{
        private long batchRecorderId;
        private ConsumerQueueDto pre;
        private CountDownLatch countDownLatch;
        private AtomicInteger timeOutCount;

        public MsgThread(ConsumerQueueDto pre, long batchRecorderId, CountDownLatch countDownLatch,
                         AtomicInteger timeOutCount) {
            this.batchRecorderId = batchRecorderId;
            this.pre = pre;
            this.countDownLatch = countDownLatch;
            this.timeOutCount = timeOutCount;
        }
        @Override
        public void run() {
            this.timeOutCount.incrementAndGet();
            taskCounter.incrementAndGet();
            BatchRecordItem batchRecorderItem = null;
            long maxId = 0;
            try {
                if (isRunning && checkOffsetVersion(pre)) {
                    maxId = threadExcute(pre, countDownLatch);
                    updateOffset(pre, maxId);
                } else {
                    countDownLatch.countDown();
                }
            } catch (Throwable e) {
                log.error("", e);
            }
            synchronized (lockObject) {
                batchRecorderItem = batchRecorder.end(batchRecorderId, maxId);
                if (batchRecorderItem != null) {
                    doCommit(pre, batchRecorderItem);
                }
            }
            this.timeOutCount.decrementAndGet();
            taskCounter.decrementAndGet();
        }
    }

    private void doCommit(ConsumerQueueDto temp, BatchRecordItem batchRecorderItem) {
        doCommit(temp, batchRecorderItem, false);
    }

    private ConsumerQueueVersionDto consumerQueueVersionDto = new ConsumerQueueVersionDto();
    private AtomicLong commitVersion = new AtomicLong(0);
    private volatile long hasCommitVersion = 0;
    private long lastCommitTime = System.currentTimeMillis();

    private void doCommit(ConsumerQueueDto temp, BatchRecordItem batchRecorderItem,boolean flag) {
        if (batchRecorderItem == null) return;
        if ((iAsynSubscriber == null) && checkOffsetVersion(temp)) {
            consumerQueueVersionDto.setOffset(batchRecorderItem.getMaxId());
            consumerQueueVersionDto.setOffsetVersion(temp.getOffsetVersion());
            consumerQueueVersionDto.setQueueOffsetId(temp.getQueueOffsetId());
            consumerQueueVersionDto.setConsumerGroupName(temp.getConsumerGroupName());
            consumerQueueVersionDto.setTopicName(temp.getTopicName());
            commitVersion.incrementAndGet();
            if (mqContext.getConfig().isSynCommit() || flag) {
                CommitOffsetRequest request1 = new CommitOffsetRequest();
                List<ConsumerQueueVersionDto> queueVersionDtos = new ArrayList<>();
                request1.setQueueOffsets(queueVersionDtos);
                queueVersionDtos.add(consumerQueueVersionDto);
                TraceMessageItem item = new TraceMessageItem();
                mqResource.commitOffset(request1);
            }
        }else {
            log.error("提交偏移失败{}",temp.getOffsetVersion());
        }
    }
    private void updateOffset(ConsumerQueueDto pre, long maxId) {
        if(pre.getOffset()<maxId&&checkOffsetVersion(pre)){
            pre.setOffset(maxId);
        }
    }

}
