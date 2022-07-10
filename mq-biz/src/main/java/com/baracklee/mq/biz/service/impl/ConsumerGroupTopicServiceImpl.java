package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.SoaConfig;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;

import javax.annotation.Resource;
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
    @Resource
    SoaConfig soaConfig;

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
        return null;
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

    }

    @Override
    public void forceUpdateCache() {

    }

    @Override
    public String getCacheJson() {
        return null;
    }
}
