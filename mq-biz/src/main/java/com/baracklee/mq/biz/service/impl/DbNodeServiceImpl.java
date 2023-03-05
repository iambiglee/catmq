package com.baracklee.mq.biz.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.DbNodeRepository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.apache.ibatis.transaction.Transaction;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DbNodeServiceImpl
        extends
        AbstractBaseService<DbNodeEntity>
        implements
        DbNodeService,
        CacheUpdateService,
        PortalTimerService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    protected volatile boolean isRunning = true;
    @Resource
    DbNodeRepository dbNodeRepository;

    @Resource
    private SoaConfig soaConfig;

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private AtomicBoolean updateFlag = new AtomicBoolean(false);


    private volatile int minIdle = 0;
    private volatile int maxActive = 0;
    private volatile int minEvictableIdleTimeMillis = 0;


    protected AtomicReference<Map<String, DataSource>> cacheDataMap = new AtomicReference<>(
            new ConcurrentHashMap<>(20));
    protected AtomicReference<Map<Long, DbNodeEntity>> cacheNodeMap = new AtomicReference<>(
            new ConcurrentHashMap<>(20));
    protected AtomicReference<Map<String, List<DbNodeEntity>>> cacheNodeIpMap = new AtomicReference<>(
            new ConcurrentHashMap<>(20));


    @PostConstruct
    public void init(){
        super.setBaseRepository(dbNodeRepository);
        minIdle = soaConfig.getInitDbCount();
        maxActive = soaConfig.getMaxDbCount();
        minEvictableIdleTimeMillis = soaConfig.getDbMinEvictableIdleTimeMillis();
        start();
        registerDbConfigChanged();
    }

    private void registerDbConfigChanged() {
        soaConfig.registerChanged(new Runnable() {
            @Override
            public void run() {
                updateDbProperties();
            }
        });
    }

    private void updateDbProperties() {
        if (minEvictableIdleTimeMillis != soaConfig.getDbMinEvictableIdleTimeMillis()) {
            minEvictableIdleTimeMillis = soaConfig.getDbMinEvictableIdleTimeMillis();
            cacheDataMap.get().values().forEach(dataSource -> {
                try {
                    if (dataSource instanceof DruidDataSource) {
                        ((DruidDataSource) dataSource)
                                .setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            });
        }
        if (minIdle != soaConfig.getInitDbCount() && maxActive != soaConfig.getMaxDbCount()) {
            log.warn("dataSource_MinIdle_changed,from {} to {}", minIdle, soaConfig.getInitDbCount());
            log.warn("dataSource_MaxActive_changed,from {} to {}", maxActive, soaConfig.getMaxDbCount());
            maxActive = soaConfig.getMaxDbCount();
            minIdle = soaConfig.getInitDbCount();
            cacheDataMap.get().values().forEach(dataSource -> {
                if (dataSource instanceof DruidDataSource) {
                    ((DruidDataSource) dataSource).setMinIdle(minIdle);
                    ((DruidDataSource) dataSource).setMaxActive(maxActive);
                }
            });
        } else if (minIdle != soaConfig.getInitDbCount()) {
            log.warn("dataSource_MinIdle_changed,from {} to {}", minIdle, soaConfig.getInitDbCount());
            minIdle = soaConfig.getInitDbCount();
            cacheDataMap.get().values().forEach(dataSource -> {
                if (dataSource instanceof DruidDataSource) {
                    ((DruidDataSource) dataSource).setMinIdle(minIdle);
                }
            });
        } else if (maxActive != soaConfig.getMaxDbCount()) {
            log.warn("dataSource_MaxActive_changed,from {} to {}", maxActive, soaConfig.getMaxDbCount());
            maxActive = soaConfig.getMaxDbCount();
            cacheDataMap.get().values().forEach(dataSource -> {
                if (dataSource instanceof DruidDataSource) {
                    ((DruidDataSource) dataSource).setMaxActive(maxActive);
                }
            });
        }
    }

    private void start() {
        if(startFlag.compareAndSet(false,true)){
            updateCache();
            ScheduledExecutorService dbNodeServer = Executors.newScheduledThreadPool(1, SoaThreadFactory.create(
                    "DbNodeServer_", true));
            dbNodeServer.execute(()->{
                while (isRunning){
                    updateCache();
                    Util.sleep(soaConfig.getMqDbNodeCacheInterval());
                }
            });

        }
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
        if(updateFlag.compareAndSet(false,true)){
            try {
                if (checkChanged()) {
                    forceUpdateCache();
                }
            } finally {
                updateFlag.set(false);
            }
        }

    }

    protected volatile LastUpdateEntity lastUpdateEntity = null;

    /**
     * 检查DB是否有过修改
     * @return
     */
    private boolean checkChanged() {
        boolean flag = false;
        try {
            LastUpdateEntity temp = dbNodeRepository.getLastUpdate();
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
        } catch (Exception e) {
            log.error("DbNodeServiceImpl_",e);
        }
        if (!flag && cacheDataMap.get().size() == 0) {
            log.warn("dbNode数据为空，请注意！");
            return true;
        }
        return flag;
    }

    @Override
    public void forceUpdateCache() {
        doForceUpdateCache();
        updateQueueCache();
    }

    /**
     * 从数据库create DB， 没有就create
     *
     */
    private void doForceUpdateCache() {
        List<DbNodeEntity> data = dbNodeRepository.getAll();
        Map<String, DataSource> dbCache = new ConcurrentHashMap<>(data.size());
        //当前数据库中的值
        Map<Long, DbNodeEntity> dbNodeCache = new ConcurrentHashMap<>(data.size());
        //数据库中的IP和其的对应关系
        Map<String, List<DbNodeEntity>> dbNodeIpCache = new ConcurrentHashMap<>(data.size());
        for (DbNodeEntity dbNode : data) {
            createDataSource(dbNode,dbCache);

        }
    }

    private void createDataSource(DbNodeEntity dbNode, Map<String, DataSource> dbCache) {

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
