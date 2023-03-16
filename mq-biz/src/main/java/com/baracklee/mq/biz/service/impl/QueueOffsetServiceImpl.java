package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.QueueOffsetRepository;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class QueueOffsetServiceImpl extends AbstractBaseService<QueueOffsetEntity>
        implements
        QueueOffsetService,
        CacheUpdateService,
        TimerService,
        BrokerTimerService
{
    private Logger log = LoggerFactory.getLogger(QueueOffsetServiceImpl.class);

    @Resource
    private QueueOffsetRepository queueOffsetRepository;

    @Resource
    private SoaConfig soaConfig;
    @Resource
    private QueueService queueService;
    @Resource
    private ConsumerGroupService consumerGroupService;

    @Resource
    private UserInfoHolder userInfoHolder;
    @Resource
    private AuditLogService auditLogService;
    @Resource
    private Message01Service message01Service;

    private AtomicReference<Map<String, QueueOffsetEntity>> offsetUqMap = new AtomicReference<>(new HashMap<>(10000));

    private AtomicReference<Map<String, Map<String, List<QueueOffsetEntity>>>> cacheDataMap = new AtomicReference<>(
            new HashMap<>(2000));

    private AtomicReference<List<QueueOffsetEntity>> cacheDataList = new AtomicReference<>(new LinkedList<>());
    private AtomicReference<Map<Long, List<QueueOffsetEntity>>> queueIdQueueOffsetMap = new AtomicReference<>(
            new ConcurrentHashMap<>(10000));
    private AtomicReference<Map<Long, OffsetVersionEntity>> idOffsetMap = new AtomicReference<>(
            new ConcurrentHashMap<>(10000));
    private AtomicReference<Map<String, Set<String>>> consumerGroupEnvsMapRef = new AtomicReference<>(
            new ConcurrentHashMap<>(500));
    private AtomicReference<Map<String, List<QueueOffsetEntity>>> consumerGroupQueueOffsetMap = new AtomicReference<>(
            new ConcurrentHashMap<>(10000));
    private AtomicLong lastVersion = new AtomicLong(0);
    private AtomicBoolean startFlag = new AtomicBoolean(false);

    private Lock cacheLock = new ReentrantLock();
    private AtomicBoolean first = new AtomicBoolean(true);
    private volatile boolean isRunning = true;

    private volatile boolean isPortal = true;


    @PostConstruct
    private void init(){
        super.setBaseRepository(queueOffsetRepository);
    }



    @Override
    public void updateConsumerId(QueueOffsetEntity t1) {
        queueOffsetRepository.updateConsumerId(t1);
    }

    @Override
    public int commitOffset(QueueOffsetEntity entity) {
        return 0;
    }

    @Override
    public int commitOffsetAndUpdateVersion(QueueOffsetEntity entity) {
        return 0;
    }

    @Override
    public int commitOffsetById(QueueOffsetEntity entity) {
        return 0;
    }

    @Override
    public void deRegister(long consumerId) {

    }

    @Override
    public Map<String, Map<String, List<QueueOffsetEntity>>> getCache() {
        return null;
    }


    @Override
    public List<QueueOffsetEntity> getCacheData() {
        return null;
    }

    @Override
    public Map<String, List<QueueOffsetEntity>> getConsumerGroupQueueOffsetMap() {
        return null;
    }

    @Override
    public Map<String, Set<String>> getSubEnvs() {
        return null;
    }

    @Override
    public List<QueueOffsetEntity> getByConsumerGroupIds(List<Long> consumerGroupIds) {
        if (CollectionUtils.isEmpty(consumerGroupIds)) {
            return new ArrayList<>();
        }
        return queueOffsetRepository.getByConsumerGroupIds(consumerGroupIds);
    }


    @Override
    /**
     * @TODO 11月17日 写到这里
     */
    public BaseUiResponse createQueueOffset(ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        List<QueueEntity> queueEntityList = queueService.getQueuesByTopicId(consumerGroupTopicEntity.getTopicId());
        ConsumerGroupEntity consumerGroup = consumerGroupService.get(consumerGroupTopicEntity.getConsumerGroupId());
        for (QueueEntity queueEntity : queueEntityList) {
            QueueOffsetEntity queueOffsetEntity = new QueueOffsetEntity();
            queueOffsetEntity.setConsumerGroupId(consumerGroup.getId());
            queueOffsetEntity.setConsumerGroupName(consumerGroup.getName());
            // 设置消费者组的原始name
            queueOffsetEntity.setOriginConsumerGroupName(consumerGroup.getOriginName());
            // 设置消费者组的消费模式
            queueOffsetEntity.setConsumerGroupMode(consumerGroup.getMode());
            queueOffsetEntity.setTopicId(consumerGroupTopicEntity.getTopicId());
            queueOffsetEntity.setTopicName(consumerGroupTopicEntity.getTopicName());
            queueOffsetEntity.setOriginTopicName(consumerGroupTopicEntity.getOriginTopicName());
            queueOffsetEntity.setTopicType(consumerGroupTopicEntity.getTopicType());
            queueOffsetEntity.setQueueId(queueEntity.getId());
            queueOffsetEntity.setSubEnv(consumerGroup.getSubEnv());
            queueOffsetEntity
                    .setDbInfo(queueEntity.getIp() + " | " + queueEntity.getDbName() + " | " + queueEntity.getTbName());
            String userId = userInfoHolder.getUserId();
            long maxId =0;
            queueOffsetEntity.setInsertBy(userId);
            message01Service.setDbId(queueEntity.getDbNodeId());
            Message01Entity message01Entity=message01Service.getMaxIdMsg(queueEntity.getTbName());
            if(message01Entity==null){
                message01Service.setDbId(queueEntity.getDbNodeId());
                maxId = queueService.getMaxId(queueEntity.getId(), queueEntity.getTbName());
            }else {
                maxId=message01Entity.getId()+1;
            }
            // 正常topic的起始偏移为：当前的最大Id
            if (consumerGroupTopicEntity.getTopicType() == 1) {
                queueOffsetEntity.setOffset(maxId - 1);
                queueOffsetEntity.setStartOffset(maxId - 1);
            }

            if (!getUqCache().containsKey(queueOffsetEntity.getConsumerGroupName() + "_"
                    + queueOffsetEntity.getTopicName() + "_" + queueOffsetEntity.getQueueId())) {
                insert(queueOffsetEntity);
                System.out.println(ConsumerGroupEntity.TABLE_NAME+
                        consumerGroupTopicEntity.getConsumerGroupId()+
                        "添加" + consumerGroupTopicEntity.getConsumerGroupName() + "订阅"
                                + consumerGroupTopicEntity.getTopicName() + "时的起始偏移："
                                + queueOffsetEntity.getStartOffset() + "。添加订阅时增加queueOffset："
                                + JsonUtil.toJson(queueOffsetEntity) + " 同时queue信息为：" + JsonUtil.toJson(queueEntity));

            } else {
                System.out.println(ConsumerGroupEntity.TABLE_NAME+
                        consumerGroupTopicEntity.getConsumerGroupId()+consumerGroupTopicEntity.getConsumerGroupName()
                                + "已经订阅了" + consumerGroupTopicEntity.getTopicName());
            }
        }
        return new BaseUiResponse();
    }

    @Override
    public long countBy(Map<String, Object> conditionMap) {
        return 0;
    }

    @Override
    public List<QueueOffsetEntity> getListBy(Map<String, Object> conditionMap, long page, long pageSize) {
        return null;
    }

    @Override
    public long getOffsetSumByIds(List<Long> ids) {
        return 0;
    }

    @Override
    public Map<String, QueueOffsetEntity> getUqCache() {
        // TODO Auto-generated method stub
        // return offsetUqMap.get();

        Map<String, QueueOffsetEntity> rs = offsetUqMap.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = offsetUqMap.get();
                if (rs.size() == 0) {
                    if (first.compareAndSet(true, false)) {
                        updateCache();
                    }
                    rs = offsetUqMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public Map<Long, List<QueueOffsetEntity>> getQueueIdQueueOffsetMap() {
        return null;
    }

    @Override
    public Map<Long, OffsetVersionEntity> getOffsetVersion() {
        return null;
    }

    @Override
    public List<QueueOffsetEntity> getUnSubscribeData() {
        return null;
    }

    @Override
    public List<QueueOffsetEntity> getAllBasic() {
        return null;
    }

    @Override
    public long getLastVersion() {
        return 0;
    }

    private AtomicBoolean updateFlag = new AtomicBoolean(false);

    @Override
    public void updateCache() {
        if (updateFlag.compareAndSet(false, true)) {
            // 如果是portal 界面则强制更新
            if (checkChanged() || isPortal) {
                forceUpdateCache();
            }
            updateFlag.set(false);
        }
    }

    private volatile LastUpdateEntity lastUpdateEntity = null;
    private long lastTime = System.currentTimeMillis();

    private boolean checkChanged() {
        boolean flag = doCheckChanged();
        if (!flag) {
            if (System.currentTimeMillis() - lastTime > soaConfig.getMqMetaRebuildMaxInterval()) {
                lastTime = System.currentTimeMillis();
                return true;
            }
        } else {
            lastTime = System.currentTimeMillis();
        }
        return flag;
    }

    private boolean doCheckChanged() {
        updateOffsetCache();
        boolean flag = false;
        try {
            LastUpdateEntity temp = queueOffsetRepository.getLastUpdate();
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
        } catch (Exception ignored) {
        }
        if (!flag && cacheDataMap.get().size() == 0) {
            log.warn("queueOffset数据为空，请注意！");
            return true;
        }
        return flag;
    }

    private void updateOffsetCache() {
        List<OffsetVersionEntity> lstData = queueOffsetRepository.getOffsetVersion();
        Map<Long, OffsetVersionEntity> dataCache = new ConcurrentHashMap<>(10000);
        lstData.forEach(t1 -> {
            dataCache.put(t1.getId(), t1);
        });
        idOffsetMap.set(dataCache);
    }

    @Override
    public void forceUpdateCache() {
        try {
            // List<QueueOffsetEntity> data =
            // queueOffsetRepository.getAllBasic();
            List<QueueOffsetEntity> data;
            if (isPortal) {
                data = queueOffsetRepository.getAll();
            } else {
                data = queueOffsetRepository.getAllBasic();
            }
            MqReadMap<String, Map<String, List<QueueOffsetEntity>>> cacheMap = new MqReadMap<>(2000);
            MqReadMap<String, QueueOffsetEntity> offsetUqMap1 = new MqReadMap<>(data.size());
            MqReadMap<Long, List<QueueOffsetEntity>> queueIdQueueOffsetMap1 = new MqReadMap<>(data.size());
            MqReadMap<String, List<QueueOffsetEntity>> consumerGroupQueueOffsetMap1 = new MqReadMap<>(data.size());
            // Map<String, QueueOffsetEntity> offsetUqMap1 = offsetUqMap.get();
            Map<String, Set<String>> consumerGroupEnvs = new HashMap<>();
            if (!CollectionUtils.isEmpty(data)) {
                data.forEach(t1 -> {
                    if (!StringUtils.isEmpty(t1.getConsumerGroupName()) && !StringUtils.isEmpty(t1.getTopicName())) {
                        if (!cacheMap.containsKey(t1.getConsumerGroupName())) {
                            cacheMap.put(t1.getConsumerGroupName(), new HashMap<>());
                        }
                        if (!cacheMap.get(t1.getConsumerGroupName()).containsKey(t1.getTopicName())) {
                            cacheMap.get(t1.getConsumerGroupName()).put(t1.getTopicName(), new ArrayList<>());
                        }
                        cacheMap.get(t1.getConsumerGroupName()).get(t1.getTopicName()).add(t1);
                    }
                    offsetUqMap1.put(t1.getConsumerGroupName() + "_" + t1.getTopicName() + "_" + t1.getQueueId(), t1);

                    if (!queueIdQueueOffsetMap1.containsKey(t1.getQueueId())) {
                        queueIdQueueOffsetMap1.put(t1.getQueueId(), new ArrayList<>());
                    }
                    queueIdQueueOffsetMap1.get(t1.getQueueId()).add(t1);

                    if (!consumerGroupQueueOffsetMap1.containsKey(t1.getConsumerGroupName())) {
                        consumerGroupQueueOffsetMap1.put(t1.getConsumerGroupName(), new ArrayList<>());
                    }
                    consumerGroupQueueOffsetMap1.get(t1.getConsumerGroupName()).add(t1);

                    if (!soaConfig.isPro() ) {
                        if (t1.getConsumerId() > 0 && !Util.isEmpty(t1.getOriginConsumerGroupName())) {
                            if (t1.getConsumerGroupMode() == 2) {
                                if (!consumerGroupEnvs.containsKey(t1.getConsumerGroupName())) {
                                    consumerGroupEnvs.put(t1.getConsumerGroupName(), new HashSet<>());
                                }
                                consumerGroupEnvs.get(t1.getConsumerGroupName()).add(t1.getSubEnv());
                            } else {
                                if (!consumerGroupEnvs.containsKey(t1.getOriginConsumerGroupName())) {
                                    consumerGroupEnvs.put(t1.getOriginConsumerGroupName(), new HashSet<>());
                                }
                                consumerGroupEnvs.get(t1.getOriginConsumerGroupName()).add(t1.getSubEnv());
                            }
                        }
                    }
                });
            }
            if (!soaConfig.isPro() ) {
                consumerGroupEnvsMapRef.set(consumerGroupEnvs);
            }
            cacheMap.setOnlyRead();
            offsetUqMap1.setOnlyRead();
            if (cacheMap.size() > 0 && data.size() > 0 && offsetUqMap1.size() > 0 && queueIdQueueOffsetMap1.size() > 0
                    && consumerGroupQueueOffsetMap1.size() > 0) {
                cacheDataMap.set(cacheMap);
                cacheDataList.set(data);
                offsetUqMap.set(offsetUqMap1);
                queueIdQueueOffsetMap.set(queueIdQueueOffsetMap1);
                consumerGroupQueueOffsetMap.set(consumerGroupQueueOffsetMap1);
            }else {
                lastUpdateEntity = null;
            }
            lastVersion.incrementAndGet();
        } catch (Exception ignored) {

        }
    }

    @Override
    public String getCacheJson() {
        return JsonUtil.toJsonNull(getCacheData());
    }

    @Override
    public void deleteByConsumerGroupId(long id) {
        queueOffsetRepository.deleteByConsumerGroupId(id);
    }

    @Override
    public void deleteByConsumerGroupIdAndOriginTopicName(ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        queueOffsetRepository.deleteByConsumerGroupIdAndOriginTopicName(consumerGroupTopicEntity.getConsumerGroupId(),
                consumerGroupTopicEntity.getOriginTopicName());
        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, consumerGroupTopicEntity.getConsumerGroupId(),
                "取消" + consumerGroupTopicEntity.getConsumerGroupName() + "对" + consumerGroupTopicEntity.getTopicName()
                        + "订阅时，删除queueOffset，对应的consumerGroupTopic为：" + JsonUtil.toJson(consumerGroupTopicEntity));
    }

    @Override
    public List<QueueOffsetEntity> getByConsumerGroupTopic(long consumerGroupId, long topicId) {
        return queueOffsetRepository.getByConsumerGroupTopic(consumerGroupId, topicId);
    }

    @Override
    public void updateStopFlag(long id, int stopFlag, String updateBy) {
        queueOffsetRepository.updateStopFlag(id,stopFlag,updateBy);
    }

    @Override
    public int updateQueueOffset(Map<String, Object> parameterMap) {
        return queueOffsetRepository.updateQueueOffset(parameterMap);
    }

    @Override
    public void setConsumerIdsToNull(List<Long> consumerIds) {
        queueOffsetRepository.setConsumserIdsToNull(consumerIds);
    }


    @Override
    public void startBroker() {
        isPortal=true;
    }

    @Override
    public void stopBroker() {

    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            ExecutorService executorService = Executors.newSingleThreadExecutor(SoaThreadFactory.create("queue_offset_service", true));
            // updateCache();
            executorService.execute(() -> {
                while (isRunning) {
                    updateCache();
                    Util.sleep(soaConfig.getMqQueueOffsetCacheInterval());
                }
            });
        }
    }

    @Override
    public void stop() {
        isRunning=false;
    }

    @Override
    public String info() {
        return null;
    }
}
