package com.baracklee.mq.biz.service.impl;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.plugin.DruidConnectionFilter;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.DbUtil;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.DbNodeRepository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.service.CacheUpdateService;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.ui.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DbNodeServiceImpl
        extends
        AbstractBaseService<DbNodeEntity>
        implements
        DbNodeService,
        CacheUpdateService,
        PortalTimerService ,
        TimerService {

    private static final Logger log = LoggerFactory.getLogger(DbNodeServiceImpl.class);

    protected volatile boolean isRunning = true;
    @Resource
    DbNodeRepository dbNodeRepository;
    private DataSourceFactory dataSourceFactory;

    @Resource
    private SoaConfig soaConfig;

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    protected Map<String, Boolean> dbCreated = new ConcurrentHashMap<>();
    protected volatile boolean isPortal = false;


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
        // TODO Auto-generated method stub
        dataSourceFactory = DruidDataSource::new;
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

    public void updateDbProperties() {
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

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            updateCache();
            ExecutorService dbNodeServer = Executors.newSingleThreadExecutor( SoaThreadFactory.create(
                    "DbNodeServer_", true));
            dbNodeServer.execute(()->{
                while (isRunning){
                    updateCache();
                    Util.sleep(soaConfig.getMqDbNodeCacheInterval());
                }
            });

        }
    }

    @Override
    public void stop() {
        isRunning=false;
    }

    private DruidDataSource createDataSouce(){
        return new DruidDataSource();
    }

    private Lock cacheLock = new ReentrantLock();

    @Override
    public Map<Long, DbNodeEntity> getCache() {
        // return cacheNodeMap.get();
        Map<Long, DbNodeEntity> rs = cacheNodeMap.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = cacheNodeMap.get();
                if (rs.size() == 0) {
                    updateCache();
                    rs = cacheNodeMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public Map<String, List<DbNodeEntity>> getCacheByIp() {
        Map<String, List<DbNodeEntity>> nodeByIp = cacheNodeIpMap.get();
        if (nodeByIp.size() == 0) {
            cacheLock.lock();
            try {
                nodeByIp = cacheNodeIpMap.get();
                if (nodeByIp.size() == 0) {
                    updateCache();
                    nodeByIp = cacheNodeIpMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return nodeByIp;
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
    public boolean checkChanged() {
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
        //不使用缓存了, do nothing
        doForceUpdateCache();
        updateQueueCache();
    }
    ExecutorService executor = Executors.newSingleThreadExecutor(SoaThreadFactory.create(
            "DbNodeServer_", true));
    @Autowired
    private QueueService queueService;
    private void updateQueueCache() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                queueService.forceUpdateCache();
            }
        });
    }

    /**
     * 从数据库create DB， 没有就create
     * 不使用缓存了, do nothing
     *
     * 2023年5月31日15:53:35 还是要添加，Datasource 的数据还是放在缓存里面好一些
     */
    private void doForceUpdateCache() {
        try {
            List<DbNodeEntity> data = dbNodeRepository.getAll();
            Map<String, DataSource> dbCache = new ConcurrentHashMap<>(data.size());
            Map<Long, DbNodeEntity> dbNodeCache = new ConcurrentHashMap<>(data.size());
            Map<String, List<DbNodeEntity>> dbNodeIpCache = new ConcurrentHashMap<>(data.size());
            data.forEach(t1 -> {
                try {
                    createDataSource(t1, dbCache);
                    dbNodeCache.put(t1.getId(), t1);
                    if (dbNodeIpCache.containsKey(t1.getIp())) {
                        dbNodeIpCache.get(t1.getIp()).add(t1);
                    } else {
                        List<DbNodeEntity> list = new ArrayList<>();
                        list.add(t1);
                        dbNodeIpCache.put(t1.getIp(), list);
                    }
                } catch (Exception ignored) {

                }
            });
            cacheNodeMap.set(dbNodeCache);
            cacheNodeIpMap.set(dbNodeIpCache);
            cacheDataMap.set(dbCache);
        } catch (Exception e) {
            log.error("dbNodeCache", e);
            lastUpdateEntity = null;
        }
    }

    private void createDataSource(DbNodeEntity dbNode, Map<String, DataSource> dbCache) {
        String dbInfo = getConKey(dbNode, true);
        if(!dbCreated.containsKey(dbInfo)){
            synchronized (DbNodeServiceImpl.class){
                if (!dbCreated.containsKey(dbInfo)){
                    initDatasource(dbNode,dbInfo,dbCache,true);
                }
            }
        }
        if(!dbCache.containsKey(dbInfo)&&cacheDataMap.get().containsKey(dbInfo)){
            dbCache.put(dbInfo,cacheDataMap.get().get(dbInfo));
        }
        if (hasSlave(dbNode)){
            dbInfo = getConKey(dbNode, false);
            if (!dbCreated.containsKey(dbInfo)) {
                synchronized (DbNodeServiceImpl.class) {
                    if (!dbCreated.containsKey(dbInfo)) {
                        initDatasource(dbNode, dbInfo, dbCache, false);
                    }
                }
            }
        }

    }

    private void initDatasource(DbNodeEntity dbNode, String dbInfo, Map<String, DataSource> dbCache, boolean isMaster) {
        try {
            // if (soaConfig.isUseDruid())
            {
                DruidDataSource dataSource = dataSourceFactory.createDataSource();
                dataSource.setDriverClassName("com.mysql.jdbc.Driver");
                if (isMaster) {
                    dataSource.setUsername(dbNode.getDbUserName());
                    dataSource.setPassword(dbNode.getDbPass());
                } else {
                    dataSource.setUsername(dbNode.getDbUserNameBak());
                    dataSource.setPassword(dbNode.getDbPassBak());
                }
                // dataSource.setUrl(t1.getConStr());
                dataSource.setUrl(getCon(dbNode, isMaster));
                dataSource.setInitialSize(minIdle);
                dataSource.setMinIdle(minIdle);
                dataSource.setMaxActive(maxActive);
                dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
                dataSource.setConnectionInitSqls(Arrays.asList("set names utf8mb4;"));
                List<Filter> filters = new ArrayList<Filter>();
                filters.add(new DruidConnectionFilter(DbUtil.getDbIp(dataSource.getUrl())));
                dataSource.setProxyFilters(filters);
                dataSource.init();
                dbCreated.put(dbInfo, true);
                log.info(dataSource.getUrl() + "数据源创建成功！dataSource_created");
                dbCache.put(dbInfo, dataSource);
            }
        } catch (Exception e) {
            log.error("initDataSource_error", e);
        }
    }

    private String getCon(DbNodeEntity dbNode, boolean isMaster) {
        String timeOutF = "&connectTimeout=%s&socketTimeout=%s";

        return doGetCon(dbNode, isMaster)
                + String.format(timeOutF, soaConfig.getConnectTimeout(), soaConfig.getSocketTimeout());

    }

    private String doGetCon(DbNodeEntity dbNode, boolean isMaster) {
        String conF = "jdbc:mysql://%s:%s/information_schema?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=CONVERT_TO_NULL&useSSL=false&rewriteBatchedStatements=true";
        if (isMaster) {
            return String.format(conF, dbNode.getIp(), dbNode.getPort());
        } else {
            return String.format(conF, dbNode.getIpBak(), dbNode.getPortBak());
        }
    }

    public String getConKey(DbNodeEntity t1, boolean isMaster) {
        if (isMaster) {
            return String.format("%s|%s|%s|%s", t1.getIp(), t1.getPort(), t1.getDbUserName(), t1.getDbPass());
        } else {
            return String.format("%s|%s|%s|%s", t1.getIpBak(), t1.getPortBak(), t1.getDbUserNameBak(),
                    t1.getDbPassBak());
        }
    }

    @Override
    public String getCacheJson() {
        return null;
    }

    @Override
    public void createDataSource(DbNodeEntity t1) {
        createDataSource(t1,cacheDataMap.get());
    }

    @Override
    public void checkDataSource(DbNodeEntity dbNodeEntity) {
        try {
            // 检查master
            checkMaster(dbNodeEntity);
            checkSlave(dbNodeEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkSlave(DbNodeEntity dbNodeEntity) throws SQLException {
        // 检查slave
        if (hasSlave(dbNodeEntity)) {
            DruidDataSource dataSource  = dataSourceFactory.createDataSource();
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUsername(dbNodeEntity.getDbUserNameBak());
            dataSource.setPassword(dbNodeEntity.getDbPassBak());
            dataSource.setUrl(getCon(dbNodeEntity, false));
            dataSource.setInitialSize(1);
            dataSource.setMinIdle(0);
            dataSource.setMaxActive(1);
            List<Filter> filters = new ArrayList<Filter>();
            filters.add(new DruidConnectionFilter(DbUtil.getDbIp(dataSource.getUrl())));
            dataSource.setProxyFilters(filters);
            dataSource.init();
            dataSource = null;
        }
    }

    private void checkMaster(DbNodeEntity dbNodeEntity) throws SQLException {
        DruidDataSource dataSource = dataSourceFactory.createDataSource();

        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername(dbNodeEntity.getDbUserName());
        dataSource.setPassword(dbNodeEntity.getDbPass());
        dataSource.setUrl(getCon(dbNodeEntity, true));
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(0);
        dataSource.setMaxActive(1);
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DruidConnectionFilter(DbUtil.getDbIp(dataSource.getUrl())));
        dataSource.setProxyFilters(filters);
        dataSource.init();
        dataSource = null;
    }

    @Override
    public DataSource getDataSource(long id, boolean isMaster) {
        Map<String, DataSource> cache = cacheDataMap.get();
        Map<Long, DbNodeEntity> data = cacheNodeMap.get();
        if (!isPortal) {
            isMaster = true;
        }
        // 如果没有备份配置，则转换为主库
        if (!isMaster && data.containsKey(id) && !hasSlave(data.get(id))) {
            isMaster = true;
        }
        if (!isMaster && "0".equals(soaConfig.getDbMasterSlave())) {
            isMaster = true;
        }
        if (data.containsKey(id)) {
            String key = getConKey(data.get(id), isMaster);
            if (cache.containsKey(key)) {
                // log.info("dbUrl is "+cache.get(key).getUrl());
                return cache.get(key);
            } else {
                key = getConKey(data.get(id), true);
                if (cache.containsKey(key)) {
                    // log.info("dbUrl is "+cache.get(key).getUrl());
                    return cache.get(key);
                }
            }
        }
        log.error("dbNode_is_" + id + "_and_datasource_is_null_and_datasources_is_" + JsonUtil.toJson(cache.keySet()));
        return null;
        }

    @Override
    public Map<String, DataSource> getDataSource() {
        return  cacheDataMap.get();
    }

    @Override
    public boolean hasSlave(DbNodeEntity dbNodeEntity) {
        if (Util.isEmpty(dbNodeEntity.getIpBak()) || Util.isEmpty(dbNodeEntity.getDbPassBak())
                || Util.isEmpty(dbNodeEntity.getDbUserNameBak()) || dbNodeEntity.getPortBak() == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void startPortal() {
        isPortal=true;
    }

    @Override
    public void stopPortal() {
        isRunning=false;
    }

    @Override
    public String info() {
        return null;
    }

    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }
}
