package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.Constants;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsumerGroupRbService extends AbstractTimerService{

    private Logger log = LoggerFactory.getLogger(ConsumerGroupRbService.class);
    @Resource
    private SoaConfig soaConfig;
    @Resource
    private NotifyMessageStatService notifyMessageStatService;

    private volatile NotifyMessageStatEntity messageStatEntity;
    private volatile long lastNotifyMessageId = 0;
    @Resource
    private ConsumerGroupConsumerService consumerGroupConsumerService;
    @Resource
    private ConsumerService consumerService;
    @Resource
    ConsumerGroupService consumerGroupService;
    @Resource
    QueueOffsetService queueOffsetService;

    @Resource
    NotifyMessageService notifyMessageService;


@PostConstruct
    private void init(){
        super.init(Constants.RB, soaConfig.getRbCheckInterval(), soaConfig);
        soaConfig.registerChanged(new Runnable() {
            private volatile int interval = soaConfig.getRbCheckInterval();

            @Override
            public void run() {
                if (soaConfig.getRbCheckInterval() != interval) {
                    interval = soaConfig.getRbCheckInterval();
                    updateInterval(interval);
                }
            }
        });
    }

    private boolean lastMaster= false;
    @Override
    public void dostart() {
        if(!soaConfig.isEnableRb()){return;}

        if (lastMaster != isMaster()) {
            //先看有没有数据
            if (!checkNotifyMessageStatId()) {
                //如果没有就插一个进去
                initNotifyMessageStatId();
            }
            lastMaster=isMaster();
        }
        //获取DB运行到哪里了
        long currentMaxId = getNotifyMessageId();
        if(currentMaxId==0){return;}
        
        //获取最新需要rb的group
        List<ConsumerGroupEntity> consumerGroupEntities = consumerGroupService
                .getLastRbConsumerGroup(lastNotifyMessageId, currentMaxId);
        if (CollectionUtils.isEmpty(consumerGroupEntities)) {
            return;
        }
        
        Map<Long, ConsumerGroupQuqueVo> consumerGroupMap = new HashMap<>();
        initRbData(consumerGroupEntities, consumerGroupMap);
        for(ConsumerGroupQuqueVo t1: consumerGroupMap.values()){
            log.warn(t1.consumerGroup.getName() + "开始重平衡！__version_is_" + t1.consumerGroup.getVersion());
            rb(t1);
            for (int i = 0; i < 3; i++) {
                try {
                    if(isMaster()){
                        consumerGroupService.rb(t1.queueOffsets);
                    }
                    break;
                } catch (Exception e) {
                    log.error("doCheckRebalance_error", e);
                    Util.sleep(5000);
                }
            }
            log.warn(t1.consumerGroup.getName() + "重平衡完毕！__version_is_" + t1.consumerGroup.getVersion());
        }
        updateNotifyMessageId(currentMaxId);
        int count = consumerGroupConsumerService.deleteUnActiveConsumer();
        if (count > 0) {
            log.info("consumerGroupConsumer_empty,count is " + count);
        }
    }

    private void updateNotifyMessageId(long currentMaxId) {
        lastNotifyMessageId = currentMaxId;
        messageStatEntity.setNotifyMessageId(lastNotifyMessageId);
        notifyMessageStatService.update(messageStatEntity);
    }

    private void rb(ConsumerGroupQuqueVo consumerGroupQuqueVo) {
        if (consumerGroupQuqueVo.consumers.size()>0){
            int count=0;
            int size=consumerGroupQuqueVo.consumers.size();
            StringBuilder sr = new StringBuilder();
            for (QueueOffsetEntity t1 : consumerGroupQuqueVo.queueOffsets) {
                ConsumerGroupConsumerEntity t2 = consumerGroupQuqueVo.consumers.get(count);
                t1.setConsumerId(t2.getConsumerId());
                t1.setConsumerName(t2.getConsumerName());
                count = (count + 1) % size;
                sr.append(String.format("将queueOffsetId%s分配给消费者%s,", t1.getId(), t2.getConsumerName()));
            }
        }else {
            //消费者可能全部下线
            for (QueueOffsetEntity t1 : consumerGroupQuqueVo.queueOffsets) {
                t1.setConsumerId(0);
                t1.setConsumerName("");
            }
        }
    }

    private void initRbData(List<ConsumerGroupEntity> consumerGroupEntities, Map<Long, ConsumerGroupQuqueVo> consumerGroupMap) {
        consumerGroupEntities.forEach(t1->{
            consumerGroupMap.put(t1.getId(),new ConsumerGroupQuqueVo());
            consumerGroupMap.get(t1.getId()).consumerGroup=t1;
        });
        ArrayList<Long> consumerGroupIds = new ArrayList<>(consumerGroupMap.keySet());
        List<ConsumerGroupConsumerEntity> consumerGroupConsumerEntities = consumerService
                .getConsumerGroupByConsumerGroupIds(consumerGroupIds);
        Map<Long,String> logMap=new HashMap<>();
        for (ConsumerGroupConsumerEntity t1 : consumerGroupConsumerEntities) {
            int consumerQuality = consumerGroupMap.get(t1.getConsumerGroupId()).consumerGroup.getConsumerQuality();
            if(consumerQuality>0
                    && consumerGroupMap.get(t1.getConsumerGroupId()).consumers.size()<consumerQuality){
                consumerGroupMap.get(t1.getConsumerGroupId()).consumers.add(t1);
            }else if(consumerQuality==0){
                consumerGroupMap.get(t1.getConsumerGroupId()).consumers.add(t1);
            }else {
                logMap.put(t1.getConsumerGroupId(),
                        consumerGroupMap.get(t1.getConsumerGroupId()).consumerGroup.getName() + "允许的最大消费者数为:"
                                + consumerQuality + ",所以" + t1.getConsumerName() + "无法消费，处于待命状态。");
            }
        }

        List<QueueOffsetEntity> queueOffsetEntities = queueOffsetService.getByConsumerGroupIds(consumerGroupIds);
        queueOffsetEntities.forEach(t1 -> {
            consumerGroupMap.get(t1.getConsumerGroupId()).queueOffsets.add(t1);
        });
    }

    private long getNotifyMessageId() {
        long currentMaxId = notifyMessageService.getRbMaxId(lastNotifyMessageId);
        if(currentMaxId==0){
            if(!checkNotifyMessageStatId()){
                initNotifyMessageStatId();
                lastNotifyMessageId=notifyMessageService.getRbMinId();
                currentMaxId=notifyMessageService.getRbMaxId();
            }
        }
        return currentMaxId;
    }

    private void initNotifyMessageStatId() {
        messageStatEntity=notifyMessageStatService.initNotifyMessageStat();
        lastNotifyMessageId=0;
    }

    private boolean checkNotifyMessageStatId() {
        messageStatEntity = notifyMessageStatService.get();
        if(messageStatEntity!=null){
            lastNotifyMessageId=messageStatEntity.getNotifyMessageId();
            return true;
        }
        return false;
    }
    class ConsumerGroupQuqueVo {
        public ConsumerGroupEntity consumerGroup;
        public List<ConsumerGroupConsumerEntity> consumers = new ArrayList<>();
        public List<QueueOffsetEntity> queueOffsets = new ArrayList<>();
    }

}
