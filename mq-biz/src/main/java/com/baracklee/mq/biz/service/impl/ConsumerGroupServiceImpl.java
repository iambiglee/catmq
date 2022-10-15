package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupRepository;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
    private ConsumerGroupRepository consumerGroupRepository;
    @Resource
    private NotifyMessageService notifyMessageService;

    protected AtomicReference<Map<String, ConsumerGroupEntity>> consumerGroupRefMap = new AtomicReference<>(
            new HashMap<>());
    protected AtomicReference<Map<Long, ConsumerGroupEntity>> consumerGroupByIdRefMap = new AtomicReference<>(
            new HashMap<>());
    private AtomicReference<List<String>> subEnvList = new AtomicReference<>(new ArrayList<>());


    private Lock cacheLocal=new ReentrantLock();
    @Resource
    private SoaConfig soaConfig;

    private AtomicBoolean first= new AtomicBoolean(true);
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

    }

    private Map<String, ConsumerGroupEntity> getConsumerGroupByName(String name) {
        HashMap<String, Object> conditions = new HashMap<>();
        conditions.put("originName",name);
        List<ConsumerGroupEntity> consumerGroupEntities = getList(conditions);
        Map<String, ConsumerGroupEntity> map=new HashMap<>();
        consumerGroupEntities.forEach(t->{
            map.put(t.getName(),t);
        });
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

    private void updateRbVersion(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return;
        consumerGroupRepository.updateRbVersion(ids);

    }

    @Override
    public String getCacheJson() {
        return null;
    }
}
