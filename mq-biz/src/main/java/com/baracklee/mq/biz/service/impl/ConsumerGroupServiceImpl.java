package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupRepository;

import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsumerGroupServiceImpl extends AbstractBaseService<ConsumerGroupEntity>
        implements CacheUpdateService, ConsumerGroupService{
    @PostConstruct
    protected void init() {
        super.setBaseRepository(consumerGroupRepository);
    }

    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    @Resource
    QueueOffsetService queueOffsetService;

    @Resource
    private ConsumerGroupRepository consumerGroupRepository;
    @Resource
    private NotifyMessageService notifyMessageService;

    protected AtomicReference<Map<String, ConsumerGroupEntity>> consumerGroupRefMap = new AtomicReference<>(
            new HashMap<>());
    protected AtomicReference<Map<Long, ConsumerGroupEntity>> consumerGroupByIdRefMap = new AtomicReference<>(
            new HashMap<>());
    private final AtomicReference<List<String>> subEnvList = new AtomicReference<>(new ArrayList<>());


    private Lock cacheLocal=new ReentrantLock();
    @Resource
    private SoaConfig soaConfig;

    private final AtomicBoolean first= new AtomicBoolean(true);
    @Override
    public Map<String, ConsumerGroupEntity> getCache() {
        Map<String, ConsumerGroupEntity> rs = consumerGroupRefMap.get();
        if(rs.size()==0){
            cacheLocal.lock();
            try {
                rs = consumerGroupRefMap.get();
                if (rs.size() == 0) {
                    if (first.compareAndSet(true, false)) {
                        updateCache();
                    }
                    rs = consumerGroupRefMap.get();
                }
                } finally{
                    cacheLocal.unlock();
                }
            }
        return rs;
    }

    @Override
    public void copyAndNewConsumerGroup(ConsumerGroupEntity consumerGroupEntityOld,
                                        ConsumerGroupEntity consumerGroupEntityNew) {
        consumerGroupEntityNew.setId(0);
        insert(consumerGroupEntityNew);
        Map<String, ConsumerGroupEntity> consumerGroupByName = getConsumerGroupByName(consumerGroupEntityOld.getName());
        Map<Long, Map<String, ConsumerGroupTopicEntity>> ctMap = consumerGroupTopicService.getCache();
        Map<String, ConsumerGroupTopicEntity> consumerTopics = ctMap.get(consumerGroupEntityOld.getId());
        if(consumerTopics!=null){
            for (Map.Entry<String, ConsumerGroupTopicEntity> entry : consumerTopics.entrySet()) {
                if (entry.getValue().getTopicType() == 1) {
                    ConsumerGroupTopicCreateRequest request2 = new ConsumerGroupTopicCreateRequest();
                    request2.setAlarmEmails(entry.getValue().getAlarmEmails());
                    request2.setConsumerBatchSize(entry.getValue().getConsumerBatchSize());
                    request2.setConsumerGroupId(consumerGroupEntityNew.getId());
                    request2.setConsumerGroupName(consumerGroupEntityNew.getName());
                    request2.setDelayProcessTime(entry.getValue().getDelayProcessTime());
                    request2.setDelayPullTime(entry.getValue().getMaxPullTime());
                    request2.setMaxLag(entry.getValue().getMaxLag());
                    request2.setOriginTopicName(entry.getValue().getOriginTopicName());
                    request2.setPullBatchSize(entry.getValue().getPullBatchSize());
                    request2.setRetryCount(entry.getValue().getRetryCount());
                    request2.setTag(entry.getValue().getTag());
                    request2.setThreadSize(entry.getValue().getThreadSize());
                    request2.setTopicId(entry.getValue().getTopicId());
                    request2.setTopicName(entry.getValue().getTopicName());
                    request2.setTopicType(entry.getValue().getTopicType());
                    request2.setTimeOut(entry.getValue().getTimeOut());
                    consumerGroupTopicService.subscribe(request2,consumerGroupMap);
                }
            }
        }
    }

    private Map<String, ConsumerGroupEntity> getConsumerGroupByName(String name) {
        HashMap<String, Object> conditions = new HashMap<>();
        conditions.put("originName",name);
        List<ConsumerGroupEntity> consumerGroupEntities = getList(conditions);
        Map<String, ConsumerGroupEntity> map=new HashMap<>();
        consumerGroupEntities.forEach(t-> map.put(t.getName(),t));
        return map;
    }

    @Override
    public void updateCache() {
        if(updateFlag.compareAndSet(false,true)){
            if(checkChanged()){
                forceUpdateCache();
            }
            updateFlag.set(false);
        }
    }
    protected volatile LastUpdateEntity lastUpdateEntity = null;
    protected long lastTime=System.currentTimeMillis();

    private boolean checkChanged() {
        boolean flag = doCheckChanged();
        if(!flag){
            if(System.currentTimeMillis()-lastTime>soaConfig.getMetaMqRebuildMaxInterval()){
                lastTime=System.currentTimeMillis();
                return true;
            }
        }else {
            lastTime=System.currentTimeMillis();
        }
        return flag;
    }

    private boolean doCheckChanged() {
        boolean flag=false;
        LastUpdateEntity temp = consumerGroupRepository.getLastUpdate();
        if ((lastUpdateEntity == null && temp != null) || (lastUpdateEntity != null && temp == null)) {
            lastUpdateEntity = temp;
            flag = true;
        } else if (lastUpdateEntity != null && temp != null
                && (temp.getMaxId() != lastUpdateEntity.getMaxId()
                || temp.getLastDate().getTime() != lastUpdateEntity.getLastDate().getTime()
                || temp.getCount() != lastUpdateEntity.getCount())) {
            lastUpdateEntity = temp;
            flag = true;
        }
        if (!flag && consumerGroupRefMap.get().size() == 0) {
            return true;
        }
        return flag;
    }

    @Override
    public void forceUpdateCache() {
        List<ConsumerGroupEntity> consumerGroupEntities = getList();
        HashMap<String, ConsumerGroupEntity> dataMap = new HashMap<>(consumerGroupEntities.size());
        HashMap<Long, ConsumerGroupEntity> dataIdMap = new HashMap<>(consumerGroupEntities.size());
        List<String> envList=new ArrayList<>();
        envList.add("default");
        consumerGroupEntities.forEach(t1->{
            dataMap.put(t1.getName(),t1);
            dataIdMap.put(t1.getId(),t1);
            if(!envList.contains(t1.getSubEnv())){
                envList.add(t1.getSubEnv());
            }
        });
        if(dataMap.size()>0&&dataIdMap.size()>0){
            consumerGroupRefMap.set(dataMap);
            consumerGroupByIdRefMap.set(dataIdMap);
            subEnvList.set(envList);
        }else {
            lastUpdateEntity=null;
        }
    }

    @Override
    public Map<String, ConsumerGroupEntity> getByNames(List<String> names) {
        if(CollectionUtils.isEmpty(names)) return new HashMap<>();
        List<ConsumerGroupEntity> consumerGroupEntities = consumerGroupRepository.getByNames(names);
        Map<String, ConsumerGroupEntity> map =
                consumerGroupEntities.stream().collect(Collectors.toMap(ConsumerGroupEntity::getName,
                        Function.identity()));
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notifyRb(List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)) return;
        updateRbVersion(ids);
        List<NotifyMessageEntity> notifyMessageEntities = new ArrayList<>();
        for (Long id : ids) {
            NotifyMessageEntity notifyMessageEntity = new NotifyMessageEntity();
            notifyMessageEntity.setConsumerGroupId(id);
            notifyMessageEntity.setMessageType(1);
            notifyMessageEntities.add(notifyMessageEntity);

            notifyMessageEntity = new NotifyMessageEntity();
            notifyMessageEntity.setConsumerGroupId(id);
            notifyMessageEntity.setMessageType(2);
            notifyMessageEntities.add(notifyMessageEntity);
        }
        notifyMessageService.insertBatch(notifyMessageEntities);
    }

    @Override
    public List<ConsumerGroupEntity> getLastRbConsumerGroup(long minMessageId, long maxMessageId) {
        return consumerGroupRepository.getLastConsumerGroup(minMessageId, maxMessageId, MessageType.Rb);
    }

    @Override
    public void rb(List<QueueOffsetEntity> queueOffsetEntities) {
        Map<Long, String> idsMap = new HashMap<>(30);
        List<NotifyMessageEntity> notifyMessageEntities = new ArrayList<>(30);
        queueOffsetEntities.forEach(t1->{
            idsMap.put(t1.getConsumerGroupId(), "");
            // 更新consumerid 和consumername
            queueOffsetService.updateConsumerId(t1);
        });
        //保证重平衡版本号
        updateRbVersion(new ArrayList<>(idsMap.keySet()));
        notifyMessageService.insertBatch(notifyMessageEntities);
    }

    private void updateRbVersion(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return;
        consumerGroupRepository.updateRbVersion(ids);

    }

    @Override
    public String getCacheJson() {
        return null;
    }
}
