package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.msg.Message01Repository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.Message01Entity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.Message01Service;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Message01ServiceImpl implements Message01Service {
    private final ThreadLocal<Long> dbId=new ThreadLocal<>();
    private final ThreadLocal<Boolean> isMaster=new ThreadLocal<>();
    private final AtomicInteger dbCounter=new AtomicInteger(0);

    @Resource
    private Message01Repository message01Repository;

    @Resource
    DbNodeService dbNodeService;
    @Override
    public void setDbId(long dbNodeId) {
        dbId.set(dbNodeId);
        isMaster.set(false);
        dbCounter.incrementAndGet();
    }

    @Override
    public Message01Entity getMaxIdMsg(String tbName) {
        try {
            setMaster(true);
            return message01Repository.getMaxIdMsg(getDbName() + "." + tbName);
        } finally {
            clearDbId();
        }
    }

    @Override
    public String getDbName() {
        Map<Long, DbNodeEntity> cache = dbNodeService.getCache();
        Long id = dbId.get();
        if(cache.containsKey(id)){
            return cache.get(id).getDbName();
        }
        return null;
    }

    //最大值为当前最大值的下个一值
    @Override
    public long getMaxId(String tbName) {
        setMaster(true);
        Long maxId;
        try {
            maxId = message01Repository.getMaxId(getDbName(), tbName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (maxId==null) return 1L;
       return maxId;

    }

    private void clearDbId() {
        dbId.remove();
        isMaster.remove();
        dbCounter.decrementAndGet();
    }

    private void setMaster(boolean b) {
        isMaster.set(b);
    }
}
