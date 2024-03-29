package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.EmailUtil;
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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    @Resource
    private DbNodeService dbNodeService;

    @Resource
    private TopicService topicService;
    @Resource
    private SoaConfig soaConfig;
    @Resource
    private AuditLogService uiAuditLogService;
    @Resource
    private EmailUtil emailUtil;

    private Lock cacheLock = new ReentrantLock();

    private volatile boolean isRunning = true;

    private volatile boolean isPortal = true;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
            SoaThreadFactory.create("QueueService"), new ThreadPoolExecutor.DiscardOldestPolicy());;
    private final ThreadPoolExecutor executorPortal = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(50), SoaThreadFactory.create("QueueService-portal", true),
            new ThreadPoolExecutor.DiscardOldestPolicy());;
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
        return message01Service.getMaxId(tbName);
    }

    @Override
    public void updateForDbNodeChange(String ip, String dbName, String oldIp, String oldDbName) {
        if(StringUtils.isEmpty(oldIp)||StringUtils.isEmpty(oldDbName)) return;
        queueRepository.updateForDbNodeChange(ip,dbName,oldIp,oldDbName);
    }

    @Override
    public List<String> getTableNamesByDbNode(Long dbNodeId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put("dbNodeId", dbNodeId);
        conditionMap.put("isActive", 0);
        List<QueueEntity> list = queueRepository.getList(conditionMap);
        List<String> tableNames = new ArrayList<>();
        return list.stream().map(QueueEntity::getTbName).collect(Collectors.toList());
    }

    @Override
    public List<AnalyseDto> countTopicByNodeId(Long id, Long page, Long limit) {
        Long start=(page -1)*limit;
        return queueRepository.countTopicByNodeId(id,start,limit);
    }

    @Override
    public List<AnalyseDto> getDistributedNodes(Long dbNodeId) {
        return queueRepository.getDistributedNodes(dbNodeId);
    }

    @Override
    public Map<Long, AnalyseDto> getQueueQuantity() {
        return queueRepository.getQueueQuantity().stream().collect(Collectors.toMap(AnalyseDto::getTopicId,e->e));
    }

    @Override
    public int updateMinId(Long id, Long minId) {
        return queueRepository.updateMinId(id, minId);

    }

    @Override
    public long getLastVersion() {
        return lastVersion.get();
    }

    @Override
    public void resetCache() {
        lastUpdateEntity=null;
    }

    @Override
    public Map<Long, QueueEntity> getAllQueueMap() {
        // return queueIdMapRef.get();

        Map<Long, QueueEntity> rs = queueIdMapRef.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = queueIdMapRef.get();
                if (rs.size() == 0) {
                        updateCache();
                    rs = queueIdMapRef.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public List<QueueEntity> getAllLocatedQueue() {
        // return queueList.get();
        List<QueueEntity> rs = queueList.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = queueList.get();
                if (rs.size() == 0) {
                        updateCache();
                    rs = queueList.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public List<QueueEntity> getDistributedList(List<Long> nodeIds, Long topicId) {
        return queueRepository.getDistributedList(nodeIds,topicId);
    }

    @Override
    public List<Long> getTopDistributedNodes(Long topicId) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("topicId", topicId);
        return queueRepository.getTopDistributedNodes(queryMap);
    }

    @Override
    public void updateWithLock(QueueEntity queueEntity) {
        int result = queueRepository.updateWithLock(queueEntity);
        if (result == 0) {
            log.error("并发错误，请检查队列数据");
        }
    }

    @Override
    public Map<Long, Long> getMax() {
        synchronized (QueueService.class) {
            initMax();
        }
        return queueIdMaxIdMapRef.get();
    }

    @Override
    public void deleteMessage(List<QueueEntity> queueEntities, long consumerGroupId) {
        for (QueueEntity queueEntity : queueEntities) {
            doDeleteMessage(queueEntity);
        }
    }

    /**
     * 解绑并删除失败消息
     */
    @Override
    public void doDeleteMessage(QueueEntity queueEntity) {

        truncateQueueProperty(queueEntity);
    }

    /**
     * 队列可能分布在不同的数据库上,先拿到所有Node 信息
     * 遍历所有的node, 然后拿到所有的队列, 队列最前面的10个拿到
     * 最后检查一遍, 队列的是否合规
     * @param topNum 头几个
     * @param nodeType 读写标识
     * @param topicId topic的标识
     * @return 拿到顶端还没有分配的节点
     */
    @Override
    public List<QueueEntity> getTopUndistributed(int topNum, int nodeType, Long topicId) {
        //获取可分配节点
        List<Long> preNodeIds = getPreNodeIds(topicId, nodeType);
        log.info("getTopUndistributed; preNodeIds:" + preNodeIds);
        if (CollectionUtils.isEmpty(preNodeIds)) {
            return new ArrayList<QueueEntity>();
        }
        //获取可分配队列
        List<QueueEntity> sortDbNodeIdIp = getTopUndistributedNodes(topNum, nodeType, preNodeIds);

        if (CollectionUtils.isEmpty(sortDbNodeIdIp)) {
            return new ArrayList<QueueEntity>();
        }

        Set<Long> nodeIdSet = new HashSet<>();
        Set<String> nodeIpSet = new HashSet<>();
        Map<Long, QueueEntity> queueMap = new HashMap<Long, QueueEntity>();
        //获取未分配节点下面的可分配队列，最多返回10条
        List<QueueEntity> preQueueList = getSortAndUndistributedList(sortDbNodeIdIp, nodeType);

        Map<Long, DbNodeEntity> dbNodeCache = dbNodeService.getCache();

        int count=0;
        while (count < 5 && queueMap.size() < topNum) {
            nodeIpSet.clear();
            for (QueueEntity queueEntity : preQueueList) {
                // 选出可分配的队列
                // if (!nodeIdSet.contains(queueEntity.getDbNodeId())) {
                if (!nodeIdSet.contains(queueEntity.getDbNodeId()) && !nodeIpSet.contains(queueEntity.getIp())
                        && !queueMap.containsKey(queueEntity.getId()) && queueMap.size() < topNum
                        && checkWrite(queueEntity, dbNodeCache)) {
                    uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                            String.format("节点[%d]上的队列[%d]获得资格", queueEntity.getDbNodeId(), queueEntity.getId()));
                    nodeIdSet.add(queueEntity.getDbNodeId());
                    nodeIpSet.add(queueEntity.getIp());
                    queueMap.put(queueEntity.getId(), queueEntity);
                }
            }
            count++;
        }

            return new ArrayList<>(queueMap.values());
    }

    /**
     * 所有可分配节点的未分配队列
     * @return 返回前10个
     */
    private List<QueueEntity> getSortAndUndistributedList(List<QueueEntity> sortDbNodeIdIp, int nodeType) {
        List<QueueEntity> allUnLocatedQueue = queueRepository.getUndistributedListByNodeIds(
                sortDbNodeIdIp.stream().map(QueueEntity::getDbNodeId).collect(Collectors.toList()), nodeType);
        List<QueueEntity> rs = new ArrayList<QueueEntity>(allUnLocatedQueue.size());
        Map<Long, List<QueueEntity>> qMap = new HashMap<Long, List<QueueEntity>>();
        allUnLocatedQueue.forEach(t1 -> {
            if (!qMap.containsKey(t1.getDbNodeId())) {
                qMap.put(t1.getDbNodeId(), new ArrayList<QueueEntity>(10));
            }
            if (qMap.get(t1.getDbNodeId()).size() < 10) {
                qMap.get(t1.getDbNodeId()).add(t1);
            }
        });
        sortDbNodeIdIp.forEach(t1 -> {
            if (qMap.containsKey(t1.getDbNodeId())) {
                rs.addAll(qMap.get(t1.getDbNodeId()));
            }
        });
        return rs;
    }

    private List<QueueEntity> getTopUndistributedNodes(int topNum, int nodeType, List<Long> nodeIds) {
        Map<String, Object> queryMap = new HashMap<>();
        // queryMap.put("topNum", topNum);
        queryMap.put("nodeType", nodeType);
        queryMap.put("nodeIds", nodeIds);
        return queueRepository.getTopUndistributedNodes(queryMap);
        
    }

    private List<Long> getPreNodeIds(Long topicId, int nodeType) {
        Set<Long> normalNodeIdSet = new HashSet<>();

        Map<Long, DbNodeEntity> dbNodeEntityMap = dbNodeService.getCache();
        Map<Long, QueueEntity> queueCache = getAllQueueMap();
        Collection<QueueEntity> queueEntities = queueCache.values();

        for (DbNodeEntity dbNodeEntity : dbNodeEntityMap.values()) {
            // 过滤普通节点上为读写数据库，和Topic类型相匹配的节点
            if (dbNodeEntity.getNormalFlag()==1
            &&dbNodeEntity.getReadOnly()==1
            &&dbNodeEntity.getNodeType()==nodeType){
                if(queueEntities.stream().anyMatch(queueEntity ->
                        queueEntity.getDbNodeId() == dbNodeEntity.getId()
                                && queueEntity.getTopicId() == 0)){
                normalNodeIdSet.add(dbNodeEntity.getId());
                }else {
                    log.info(queueEntities.size() + "");
                }
            }
        }

        //过滤掉已经分配节点
        List<QueueEntity> queueEntityList = getQueuesByTopicId(topicId);
        List<Long> nodeIds = queueEntityList.stream().map(QueueEntity::getDbNodeId).collect(Collectors.toList());
        Set<Long> resultIds = new HashSet<>(normalNodeIdSet);
        nodeIds.forEach(resultIds::remove);
        if (!CollectionUtils.isEmpty(resultIds)) {
            return new ArrayList<Long>(resultIds);
        } else {
            // 所有基本条件符合的节点都分配过了，直接返回所有符合基本条件的节点
            return new ArrayList<Long>(normalNodeIdSet);
        }
    }

    @Override
    public void truncate(QueueEntity queueEntity) {
        uiAuditLogService.recordAudit(QueueEntity.TABLE_NAME, queueEntity.getId(),
                "待truncate 的queue为：" + JsonUtil.toJson(queueEntity));
        // 动态切换数据源
        message01Service.setDbId(queueEntity.getDbNodeId());
        // 此处需要将truncate操作变成异步操作
        message01Service.truncate(queueEntity.getTbName());
        uiAuditLogService.recordAudit(QueueEntity.TABLE_NAME, queueEntity.getId(), "truncate完成");
        truncateQueueProperty(queueEntity);
    }
    private void truncateQueueProperty(QueueEntity queueEntity) {
        queueEntity.setTopicId(0);
        queueEntity.setTopicName("");
        queueEntity.setReadOnly(1);
        queueEntity.setMinId(0L);
        update(queueEntity);
    }

    @Override
    public List<QueueEntity> getListBy(Map<String, Object> conditionMap, long page, long pageSize) {
        conditionMap.put("start1", (page - 1) * pageSize);
        conditionMap.put("offset1", pageSize);
        return queueRepository.getListBy(conditionMap);
    }

    @Override
    public long countBy(Map<String, Object> conditionMap) {
        return queueRepository.countBy(conditionMap);
    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicWriteQueue() {
        // TODO Auto-generated method stub
        // return topicWriteQueueMap.get();

        Map<String, List<QueueEntity>> rs = topicWriteQueueMap.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = topicWriteQueueMap.get();
                if (rs.size() == 0) {
                    updateCache();

                    rs = topicWriteQueueMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicQueue() {
        // TODO Auto-generated method stub
        // return topicQueueMap.get();

        Map<String, List<QueueEntity>> rs = topicQueueMap.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = topicQueueMap.get();
                if (rs.size() == 0) {
                    updateCache();
                    rs = topicQueueMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public void startBroker() {
        isPortal=false;
    }

    @Override
    public void stopBroker() {

    }

    @Override
    public void startPortal() {
        if (startPortalFlag.compareAndSet(false,true)){

            executorPortal.execute(()->{
                while (isRunning){
                    if (System.currentTimeMillis() - lastMaxTime < soaConfig.getMqQueueMaxRebuildInterval() * 1.6) {
                        initMax();
                        lastUpdateTime = System.currentTimeMillis();
                    }
                    Util.sleep(soaConfig.getMqQueueMaxRebuildInterval());
                }
            });
        }
    }

    /**
     * 获取队列的最大值
     * @return mapMap
     */
    private Map<Long, Long> initMax() {
        Map<Long, QueueEntity> data = queueIdMapRef.get();
        // key为ip+db+tb,value 为id
        Map<String, Long> queueMap = new HashMap<>(data.size());
        // key为ip，value为db node id
        Map<String, Long> dbIpIdMap = new HashMap<>(data.size());
        // 第一层的key为数据库实例的key,第二层的key为数据库名，第三层dekey为表名
        Map<Long, Long> maxMap = new ConcurrentHashMap<>(data.size());

        for (Map.Entry<Long, QueueEntity> entry : data.entrySet()) {
            queueMap.put(getKey(entry.getValue().getIp(),entry.getValue().getDbName(),entry.getValue().getTbName()),entry.getKey());
            dbIpIdMap.put(entry.getValue().getIp(),entry.getValue().getDbNodeId());
        }

        dbIpIdMap.entrySet().forEach(t1 -> {
            message01Service.setDbId(t1.getValue());
            Map<String, Map<String, Long>> maxNode = message01Service.getMaxIdByIp(t1.getKey());
            maxNode.forEach((key2, value1) -> value1.forEach((key1, value) -> {
                String key = getKey(t1.getKey(), key2, key1);
                if (queueMap.containsKey(key)) {
                    maxMap.put(queueMap.get(key), value);
                }
            }));
        });

        data.forEach((key, value) -> {
            if (!maxMap.containsKey(key)) {
                message01Service.setDbId(value.getDbNodeId());
                maxMap.put(key, message01Service.getMaxId(value.getTbName()));
            }
        });

        queueIdMaxIdMapRef.set(maxMap);
        return maxMap;
    }

    private String getKey(String ip, String db, String tb) {
        return ip + "_" + db + "_" + tb;
    }
    @Override
    public void stopPortal() {
        isRunning = false;
    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false,true)){
            updateCache();

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
        if (dbNodeCache.isEmpty()) {
            List<DbNodeEntity> dbNodes = dbNodeService.getList();
            dbNodeCache = new HashMap<>(dbNodes.size());
            for (DbNodeEntity t1 : dbNodes) {
                dbNodeCache.put(t1.getId(), t1);
            }
            log.warn("dbNodeCache_lost");
        }
        if(!CollectionUtils.isEmpty(data)&&dbNodeCache.size()>0){
            for (QueueEntity entity : data) {
                queueIdMap.put(entity.getId(),entity);
                if (StringUtils.isEmpty(entity.getTopicName())) continue;
                if (!topicQueueMap1.containsKey(entity.getTopicName())) {
                    topicQueueMap1.put(entity.getTopicName(), new ArrayList<>());
                }
                if (!topicWriteQueueMap1.containsKey(entity.getTopicName())) {
                    topicWriteQueueMap1.put(entity.getTopicName(), new ArrayList<>());
                }
                if (checkWrite(entity, dbNodeCache)) {
                    topicWriteQueueMap1.get(entity.getTopicName()).add(entity);
                }
                topicQueueMap1.get(entity.getTopicName()).add(entity);
            }
        }
        if(!isPortal||"1".equals(soaConfig.getLogPortalTopic())){
            checkTopic(topicWriteQueueMap1,topicQueueMap1,cache);
        }
        topicWriteQueueMap1.setOnlyRead();
        topicQueueMap1.setOnlyRead();
        queueIdMap.setOnlyRead();
        if (queueIdMap.size() > 0 && data.size() > 0) {
            topicWriteQueueMap.set(topicWriteQueueMap1);
            topicQueueMap.set(topicQueueMap1);
            queueIdMapRef.set(queueIdMap);
            queueList.set(data);
        } else {
            lastUpdateEntity = null;
        }
        lastVersion.incrementAndGet();
    }

    /**
     * 需要实时检查 queue 表中信息是否正确
     * @param topicWriteQueueMap1 可写队列
     * @param topicQueueMap1 有topic的队列
     * @param cache 所有的队列
     */
    private void checkTopic(MqReadMap<String, List<QueueEntity>> topicWriteQueueMap1,
                            MqReadMap<String, List<QueueEntity>> topicQueueMap1,
                            Map<String, TopicEntity> cache) {
        int topicCount = cache.size();
        for (Map.Entry<String, List<QueueEntity>> entry : topicWriteQueueMap1.entrySet()) {
            if(entry.getValue().size()==0)
                emailUtil.sendErrorMail(entry.getKey() + ",没有可以写入的队列", "topic:" + entry.getKey() + "没有可以写入的队列，请注意！");
        }

        if (topicQueueMap1.size() - topicCount > 1) {
            StringBuilder rs = new StringBuilder();
            for (String topic : topicQueueMap1.keySet()) {
                if (!topicService.NEED_DELETED_TOPIC_NANE.equals(topic)) {
                    if (!cache.containsKey(topic)) {
                        rs.append("topic:" + topic + "在queue中，不在topic表中，请注意！\n");
                    }
                }
            }
            for (String topic : cache.keySet()) {
                if (!topicQueueMap1.containsKey(topic)) {
                    rs.append("topic:" + topic + "在topic表中，不在queue表中，请注意！\n");
                }
            }
            rs.append("因为缓存的异步性，可能会出现短暂的不一致。缓存保证最终一致性。");
            emailUtil.sendWarnMail(
                    "topic数量(" + topicCount + ")与queue中topic的数量(" + topicWriteQueueMap1.size() + ")不一致，请注意！",
                    rs.toString());
        }
    }



    private boolean checkWrite(QueueEntity temp, Map<Long, DbNodeEntity> dbNodeCache) {
        if (dbNodeCache.containsKey(temp.getDbNodeId())) {
            // 读写状态： 1读写 2只读 3不可读不可写
            if (dbNodeCache.get(temp.getDbNodeId()).getReadOnly() == 1) {
                // 读写状态：1读写 2只读
                return temp.getReadOnly() == 1;
            }
        }
        return false;
    }


    @Override
    public String getCacheJson() {
        return JsonUtil.toJsonNull(getAllQueueMap());
    }
}
