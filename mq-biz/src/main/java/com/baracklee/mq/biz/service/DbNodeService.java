package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public interface DbNodeService extends BaseService<DbNodeEntity> {
    Map<Long, DbNodeEntity> getCache();
    Map<String, List<DbNodeEntity>> getCacheByIp();
    void updateCache();
    void createDataSource(DbNodeEntity t1);
    void checkDataSource(DbNodeEntity dbNodeEntity);
    DataSource getDataSource(long id, boolean isMaster);
    Map<String, DataSource> getDataSource();
    //String getConKey(long dbNodeId);
    //String getIpFromKey(String key);
    //long getLastVersion();
    boolean hasSlave(DbNodeEntity dbNodeEntity);
}

