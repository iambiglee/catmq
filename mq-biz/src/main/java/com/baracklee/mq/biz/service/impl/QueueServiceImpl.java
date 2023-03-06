package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.QueueRepository;
import com.baracklee.mq.biz.dto.AnalyseDto;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class QueueServiceImpl extends
        AbstractBaseService<QueueEntity>
        implements
        CacheUpdateService, QueueService, TimerService, PortalTimerService, BrokerTimerService

{
    Logger log= LoggerFactory.getLogger(this.getClass());
    @Resource
    QueueRepository queueRepository;
    @Resource
    Message01Service message01Service;
    @Autowired
    private DbNodeService dbNodeService;

    @Autowired
    private TopicService topicService;
    @Autowired
    private SoaConfig soaConfig;
    @Autowired
    private AuditLogService uiAuditLogService;
    private volatile boolean isRunning = true;

    private volatile boolean isPortal = true;
    private ThreadPoolExecutor executor = null;
    private ThreadPoolExecutor executorPortal = null;
    private AtomicLong lastVersion = new AtomicLong(0);
    // 记录上次获取最大值的时间
    private volatile long lastMaxTime = System.currentTimeMillis();

    // 记录上次获取最大值的时间
    private volatile long lastUpdateTime = System.currentTimeMillis();
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private AtomicBoolean startPortalFlag = new AtomicBoolean(false);

    //封装业务层?实现CQRS
    private AtomicReference<Map<String, List<QueueEntity>>> topicQueueMap = new AtomicReference<>(
            new ConcurrentHashMap<>());

    private AtomicReference<Map<String, List<QueueEntity>>> topicWriteQueueMap = new AtomicReference<>(
            new ConcurrentHashMap<>());
    private AtomicReference<Map<Long, QueueEntity>> queueIdMapRef = new AtomicReference<>(
            new ConcurrentHashMap<>(30000));
    private AtomicReference<Map<Long, Long>> queueIdMaxIdMapRef = new AtomicReference<>(new ConcurrentHashMap<>());
    private AtomicReference<List<QueueEntity>> queueList = new AtomicReference<>(new LinkedList<>());

    @PostConstruct
    private void init(){
        super.setBaseRepository(queueRepository);
    }
    @Override
    public List<QueueEntity> getQueuesByTopicId(long topicId) {
        Map<String, Object> conditionMap=new HashMap<>();
        conditionMap.put("topicId",topicId);
        return getList(conditionMap);
    }

    @Override
    public long getMaxId(long id, String tbName) {
        long maxid=message01Service.getMaxId(tbName);
        return maxid;
    }

    @Override
    public void updateForDbNodeChange(String ip, String dbName, String oldIp, String oldDbName) {

    }

    @Override
    public List<String> getTableNamesByDbNode(Long dbNodeId) {
        return null;
    }

    @Override
    public List<AnalyseDto> countTopicByNodeId(Long id, Long page, Long limit) {
        return null;
    }

    @Override
    public List<AnalyseDto> getDistributedNodes(Long dbNodeId) {
        return null;
    }

    @Override
    public Map<Long, AnalyseDto> getQueueQuantity() {
        return null;
    }

    @Override
    public int updateMinId(Long id, Long minId) {
        return 0;
    }

    @Override
    public long getLastVersion() {
        return 0;
    }

    @Override
    public void resetCache() {

    }

    @Override
    public Map<Long, QueueEntity> getAllQueueMap() {
        return null;
    }

    @Override
    public List<QueueEntity> getAllLocatedQueue() {
        return null;
    }

    @Override
    public List<QueueEntity> getDistributedList(List<Long> nodeIds, Long topicId) {
        return null;
    }

    @Override
    public List<Long> getTopDistributedNodes(Long topicId) {
        return null;
    }

    @Override
    public void updateWithLock(QueueEntity queueEntity) {

    }

    @Override
    public Map<Long, Long> getMax() {
        return null;
    }

    @Override
    public void deleteMessage(List<QueueEntity> queueEntities, long consumerGroupId) {

    }

    @Override
    public void doDeleteMessage(QueueEntity queueEntity) {

    }

    @Override
    public List<QueueEntity> getTopUndistributed(int topNum, int nodeType, Long topicId) {
        return null;
    }

    @Override
    public void truncate(QueueEntity queueEntity) {

    }

    @Override
    public List<QueueEntity> getListBy(Map<String, Object> conditionMap, long page, long pageSize) {
        return null;
    }

    @Override
    public long countBy(Map<String, Object> conditionMap) {
        return 0;
    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicWriteQueue() {
        return null;
    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicQueue() {
        return null;
    }

    @Override
    public void startBroker() {

    }

    @Override
    public void stopBroker() {

    }

    @Override
    public void startPortal() {

    }

    @Override
    public void stopPortal() {

    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false,true)){
            updateCache();
            executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    SoaThreadFactory.create("QueueService"), new ThreadPoolExecutor.DiscardOldestPolicy());
            executor.execute(() -> {
                // 因为第一次的时候，会由topic和dbnode 触发初始化，所以自身初始化可以减少一次
                checkChanged();
                while (isRunning) {
                    try {
                        updateCache();
                        lastUpdateTime = System.currentTimeMillis();

                    } catch (Throwable e) {
                        log.error("QueueServiceImpl_initCache_error", e);
                    }
                    Util.sleep(soaConfig.getMqQueueCacheInterval());
                }
            });
        }
    }

    private boolean checkChanged() {
        return doCheckChanged();
    }
    private volatile LastUpdateEntity lastUpdateEntity = null;

    private boolean doCheckChanged() {
        boolean flag=false;
        LastUpdateEntity temp = queueRepository.getLastUpdate();
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
        if (!flag && queueIdMapRef.get().size() == 0) {
            log.warn("queue数据为空，请注意！");
            return true;
        }
        return flag;
    }

    @PreDestroy
    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public String info() {
        return null;
    }

    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    @Override
    public void updateCache() {
        if (updateFlag.compareAndSet(false, true)) {
            if (checkChanged()) {
                forceUpdateCache();
            }
            updateFlag.set(false);
        }
    }

    @Override
    public void forceUpdateCache() {
        List<QueueEntity> data = queueRepository.getAll();
        //拿topic
        Map<String, TopicEntity> cache = topicService.getCache();
        int size = cache.size();
        if (size==0){
            List<TopicEntity> topics = topicService.getList();
            cache = topics.stream().collect(Collectors.toMap(TopicEntity::getName, t -> t));
            size=cache.size();
            log.warn("topicCache_lost");
        }
        MqReadMap<String, List<QueueEntity>> topicQueueMap1 = new MqReadMap<>(size);
        MqReadMap<String, List<QueueEntity>> topicWriteQueueMap1 = new MqReadMap<>(size);
        MqReadMap<Long, QueueEntity> queueIdMap = new MqReadMap<>(data.size());
        Map<Long, DbNodeEntity> dbNodeCache = dbNodeService.getCache();
        if (dbNodeCache.size() == 0) {
            List<DbNodeEntity> dbNodes = dbNodeService.getList();
            dbNodeCache = new HashMap<>(dbNodes.size());
            for (DbNodeEntity t1 : dbNodes) {
                dbNodeCache.put(t1.getId(), t1);
            }
            log.warn("dbNodeCache_lost");
        }
    }


    @Override
    public String getCacheJson() {
        return JsonUtil.toJsonNull(getAllQueueMap());
    }
}
