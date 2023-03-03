package com.baracklee.mq.biz.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.dal.meta.DbNodeRepository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DbNodeServiceImpl
        extends
        AbstractBaseService<DbNodeEntity>
        implements
        DbNodeService,
        CacheUpdateService,
        PortalTimerService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    DbNodeRepository dbNodeRepository;

    @Resource
    private SoaConfig soaConfig;

    private volatile int minIdle = 0;
    private volatile int maxActive = 0;
    private volatile int minEvictableIdleTimeMillis = 0;

    @PostConstruct
    public void init(){
        super.setBaseRepository(dbNodeRepository);
        minIdle = soaConfig.getInitDbCount();
        maxActive = soaConfig.getMaxDbCount();
        minEvictableIdleTimeMillis = soaConfig.getDbMinEvictableIdleTimeMillis();
        start();
        registerDbConfigChanged();
    }

    private DataSource createDataSouce(){
        return new DruidDataSource();
    }

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
    public void forceUpdateCache() {

    }

    @Override
    public String getCacheJson() {
        return null;
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

    @Override
    public void startPortal() {

    }

    @Override
    public void stopPortal() {

    }

    @Override
    public String info() {
        return null;
    }
}
