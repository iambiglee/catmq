package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.DbNodeRepository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DbNodeServiceImpl extends AbstractBaseService<DbNodeEntity> implements DbNodeService {
    @Resource
    DbNodeRepository dbNodeRepository;

    @Override
    public Map<Long, DbNodeEntity> getCache() {
        List<DbNodeEntity> data = dbNodeRepository.getAll();
        Map<String, DataSource> dbCache = new ConcurrentHashMap<>(data.size());
        Map<Long, DbNodeEntity> dbNodeCache = new ConcurrentHashMap<>(data.size());
        Map<String, List<DbNodeEntity>> dbNodeIpCache = new ConcurrentHashMap<>(data.size());
        for (DbNodeEntity item : data) {
            createDataSource(item);
            dbNodeCache.put(item.getId(),item);
            if(dbNodeIpCache.containsKey(item.getIp())){
                dbNodeIpCache.get(item.getIp()).add(item);
            }else {
                List<DbNodeEntity> list = new ArrayList<>();
                list.add(item);
                dbNodeIpCache.put(item.getIp(), list);
            }
        }
        return dbNodeCache;

    }

    @Override
    public Map<String, List<DbNodeEntity>> getCacheByIp() {
        return null;
    }

    @Override
    public void updateCache() {

    }

    @Override
    public void createDataSource(DbNodeEntity t1) {

    }

    @Override
    public void checkDataSource(DbNodeEntity dbNodeEntity) {

    }

    @Override
    public DataSource getDataSource(long id, boolean isMaster) {
        return null;
    }

    @Override
    public Map<String, DataSource> getDataSource() {
        return null;
    }

    @Override
    public boolean hasSlave(DbNodeEntity dbNodeEntity) {
        return false;
    }
}
