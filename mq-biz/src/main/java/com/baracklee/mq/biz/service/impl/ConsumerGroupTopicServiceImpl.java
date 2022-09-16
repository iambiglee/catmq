package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupTopicRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConsumerGroupTopicServiceImpl
        extends AbstractBaseService<ConsumerGroupTopicEntity>
        implements ConsumerGroupTopicService , CacheUpdateService
{
    protected AtomicReference<Map<Long, Map<String, ConsumerGroupTopicEntity>>> consumerGroupTopicRefMap = new AtomicReference<>(
            new HashMap<>());

    protected AtomicReference<Map<String, ConsumerGroupTopicEntity>> groupTopicRefMap = new AtomicReference<>(
            new HashMap<>());

    protected AtomicReference<Map<String, List<ConsumerGroupTopicEntity>>> topicSubscribeRefMap = new AtomicReference<>(
            new HashMap<>());

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("ConsumerGroupTopicService", true),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private Lock cacheLock = new ReentrantLock();
    protected AtomicBoolean first = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    SoaConfig soaConfig;
    @Resource
    private ConsumerGroupTopicRepository consumerGroupTopicRepository;

    //第一级key为consumergroupid，第二级key为topic名称，value为ConsumerGroupTopicEntity
    @Override
    public Map<Long, Map<String, ConsumerGroupTopicEntity>> getCache() {
        Map<Long, Map<String, ConsumerGroupTopicEntity>> rs = consumerGroupTopicRefMap.get();
        if(rs.size()==0){
            cacheLock.lock();
            try {
                rs = consumerGroupTopicRefMap.get();
                if(rs.size()==0){
                    if(first.compareAndSet(true,false)){
                        updateCache();
                    }
                    rs=consumerGroupTopicRefMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }
    private AtomicBoolean updateFlag = new AtomicBoolean(false);
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
    protected boolean checkChanged() {
        boolean flag= doCheckChanged();
        if(!flag){
            if(System.currentTimeMillis()-lastTime>soaConfig.getMetaMqRebuildMaxInterval()){
                lastTime=System.currentTimeMillis();
                return true;
            }
        }else{
            lastTime=System.currentTimeMillis();
        }
        return flag;
    }

    private boolean doCheckChanged() {
        boolean flag=false;
        LastUpdateEntity temp = consumerGroupTopicRepository.getLastUpdate();
        if((lastUpdateEntity==null&&temp!=null)||(lastUpdateEntity!=null||temp==null)){
            lastUpdateEntity=temp;
            flag=true;
        }else if (lastUpdateEntity!=null&&temp!=null&& (
                lastUpdateEntity.getMaxId()!=temp.getMaxId()
                        ||temp.getLastDate().getTime()!=lastUpdateEntity.getLastDate().getTime()
                        ||temp.getCount() != lastUpdateEntity.getCount()
                )){
            lastUpdateEntity=temp;
            flag=true;
        }
        if(!flag&&consumerGroupTopicRepository.get().size()==0){
            log.warn("consumerGroupTopic数据为空，请注意！");
            return true;}
        return flag;
    }

    @Override
    public void forceUpdateCache() {
        List<ConsumerGroupTopicEntity> consumerGroupEntities = getList();
        MqReadMap<Long, Map<String, ConsumerGroupTopicEntity>> dataMap = new MqReadMap<>(consumerGroupEntities.size()/3);
        MqReadMap<String, ConsumerGroupTopicEntity> groupTopicMap = new MqReadMap<>(consumerGroupEntities.size());
        MqReadMap<String, List<ConsumerGroupTopicEntity>> topicSubscribeMap = new MqReadMap<>(consumerGroupEntities.size());
        for (ConsumerGroupTopicEntity t1 : consumerGroupEntities) {
            if(!dataMap.containsKey(t1.getConsumerGroupId())){
                dataMap.put(t1.getConsumerGroupId(),new HashMap<>());
            }
            dataMap.get(t1.getConsumerGroupId()).put(t1.getTopicName(),t1);
            if(!groupTopicMap.containsKey(t1.getConsumerGroupName()+"_"+t1.getTopicName())){
                groupTopicMap.put(t1.getConsumerGroupName() + "_" + t1.getTopicName(), t1);
            }
            if(!topicSubscribeMap.containsKey(t1.getTopicName())){
                topicSubscribeMap.put(t1.getTopicName(),new ArrayList<>());
            }
            topicSubscribeMap.get(t1.getTopicName()).add(t1);
        }
        dataMap.setOnlyRead();
        groupTopicMap.setOnlyRead();
        consumerGroupTopicRefMap.set(dataMap);
        groupTopicRefMap.set(groupTopicMap);
        topicSubscribeRefMap.set(topicSubscribeMap);
    }

    @Override
    public String getCacheJson() {
        return null;
    }
}
