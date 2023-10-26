package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqContext;
import com.baracklee.mq.client.core.IMqGroupExecutorService;
import com.baracklee.mq.client.core.IMqQueueExecutorService;
import com.baracklee.mq.client.factory.IMqFactory;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MqGroupExecutorService implements IMqGroupExecutorService {

    private Logger log = LoggerFactory.getLogger(MqGroupExecutorService.class);

    private volatile  int versionCount=0;
    private volatile boolean isRunning =false;
    private volatile ConsumerGroupOneDto localConsumerGroup;

    private volatile Map<Long, IMqQueueExecutorService> mqEx=new ConcurrentHashMap<>();

    private MqContext mqContext;

    private IMqResource mqResource;
    private IMqFactory mqFactory=null;

    public MqGroupExecutorService() {
        this(MqClient.getContext().getMqResource());
    }

    public MqGroupExecutorService(IMqResource mqResource) {
        this.mqContext = MqClient.getContext();
        this.mqResource = mqResource;
        this.mqFactory = MqClient.getMqFactory();
    }

    //启动必须是连续三次,版本号没有发生变化才会启动
    @Override
    public void start() {
        if(!isRunning){
            versionCount++;
            if(versionCount>=mqContext.getConfig().getRbTimes()){
                log.info("retry_" + localConsumerGroup.getMeta().getName() + "_version_"
                        + localConsumerGroup.getMeta().getRbVersion() + "_retrying_" + versionCount + " of "
                        + mqContext.getConfig().getRbTimes() + " times");
                doStartQueue();
                isRunning=true;
            }
        }
    }

    private void doStartQueue() {
        if (localConsumerGroup != null && localConsumerGroup.getQueues() != null
                && localConsumerGroup.getQueues().size() > 0){
            localConsumerGroup.getQueues().values().forEach(t1->{
                IMqQueueExecutorService mqQueueExcutorService 
                        = mqFactory.createMqQueueExcutorService(localConsumerGroup.getMeta().getName(), t1);
                mqEx.put(t1.getQueueId(),mqQueueExcutorService);
                mqQueueExcutorService.start();
            });
        }
    }

    @Override
    public void close() {
        if(isRunning){
            stopQueues();
            commitMessage();
            closeQueue();
            isRunning=false;
        }
    }

    private void closeQueue() {
        if (isRunning && localConsumerGroup != null && mqEx != null && mqEx.size() > 0) {
            for (IMqQueueExecutorService value : mqEx.values()) {
                value.close();
            }
            mqEx.clear();
        }
        }

    private void commitMessage() {
        if (isRunning && localConsumerGroup != null && mqEx != null && mqEx.size() > 0) {
            CommitOffsetRequest request = new CommitOffsetRequest();
            List<ConsumerQueueVersionDto> queueVersionDtos = new ArrayList<>();
            request.setQueueOffsets(queueVersionDtos);
            mqEx.entrySet().forEach(t->{
                long start=System.currentTimeMillis();
                while (!t.getValue().hasFininshed()){
                    Util.sleep(10);
                    if (System.currentTimeMillis()-start>1000){
                        break;
                    }
                }
                ConsumerQueueVersionDto last = t.getValue().getLast();
                if(last!=null){
                    queueVersionDtos.add(last);
                }
            });
            request.setFlag(1);
            if(queueVersionDtos.size()>0){
                mqResource.commitOffset(request);
                versionCount=0;
            }
        }
    }

    private void stopQueues() {
        if(isRunning&&localConsumerGroup!=null&&mqEx!=null&&mqEx.size()>0){
            for (IMqQueueExecutorService value : mqEx.values()) {
                value.stop();
            }
        }
    }

    @Override
    public void rbOrUpdate(ConsumerGroupOneDto consumerGroupOne, String serverIp) {
        mqContext.getConsumerGroupMap().put(consumerGroupOne.getMeta().getName(),consumerGroupOne);
        if(localConsumerGroup==null) {
            localConsumerGroup = new ConsumerGroupOneDto();
            localConsumerGroup.setMeta(consumerGroupOne.getMeta());
            if (consumerGroupOne.getQueues() != null) {
                localConsumerGroup.setQueues(new ConcurrentHashMap<>(consumerGroupOne.getQueues()));
            }
            versionCount = 0;
        }
            if (consumerGroupOne.getMeta().getRbVersion() > localConsumerGroup.getMeta().getRbVersion()) {
                doRb(consumerGroupOne, serverIp);
            }
            if (consumerGroupOne.getMeta().getMetaVersion() > localConsumerGroup.getMeta().getMetaVersion()) {
                log.info("meta data changed,元数据发生变更" + consumerGroupOne.getMeta().getName());
                String preJson = JsonUtil.toJson(localConsumerGroup);
                localConsumerGroup.getMeta().setMetaVersion(consumerGroupOne.getMeta().getMetaVersion());
                updateMeta(consumerGroupOne);
            }
            localConsumerGroup.getMeta().setVersion(consumerGroupOne.getMeta().getVersion());
        }


    private void updateMeta(ConsumerGroupOneDto consumerGroupOne) {
        mqContext.getConsumerGroupMap().put(consumerGroupOne.getMeta().getName(),consumerGroupOne);
        if(consumerGroupOne.getQueues()!=null){
            for (Map.Entry<Long, ConsumerQueueDto> entry : consumerGroupOne.getQueues().entrySet()) {
                if(localConsumerGroup.getQueues()==null) localConsumerGroup.setQueues(new ConcurrentHashMap<>(15));
                if(entry.getKey() == entry.getValue().getQueueId()){
                    localConsumerGroup.getQueues().put(entry.getKey(),entry.getValue());
                    if (mqEx.containsKey(entry.getKey())){
                        mqEx.get(entry.getKey()).updateQueueMeta(entry.getValue());
                    }
                }
            }
        }
    }

    private void doRb(ConsumerGroupOneDto consumerGroupOne, String serverIp) {
        log.info("raised rebalance,发生重平衡" + consumerGroupOne.getMeta().getName());
        versionCount=0;
        //重平衡版本号不一致的时候,需要先停止当前服务
        if(isRunning) {
            log.info("commit offset,提交偏移" + consumerGroupOne.getMeta().getName());
            close();
            //等待系统中的消费被消费完成,防止重复消费
            if (mqContext.getConfig().getRbTimes() <= 1) {
                Util.sleep(1000);
            }
        }
            localConsumerGroup.getMeta().setRbVersion(consumerGroupOne.getMeta().getRbVersion());
            if (localConsumerGroup.getQueues() != null) {
                localConsumerGroup.setQueues(new ConcurrentHashMap<>(consumerGroupOne.getQueues()));
            } else {
                localConsumerGroup.setQueues(new ConcurrentHashMap<>(15));
            }
            isRunning=false;

        log.info("update offset version,更新重平衡版本号" + consumerGroupOne.getMeta().getName());
        localConsumerGroup.getMeta().setRbVersion(consumerGroupOne.getMeta().getRbVersion());

    }

    @Override
    public Map<Long, IMqQueueExecutorService> getQueueEx() {
        return mqEx;
    }
}
