package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.ui.vo.QueueOffsetVo;
import com.baracklee.ui.spi.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Barack Lee
 */
@Service
public class UiQueueOffsetService implements TimerService {

    private QueueOffsetService queueOffsetService;

    private AuditLogService auditLogService;

    private ConsumerGroupService consumerGroupService;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private UserInfoHolder userInfoHolder;

    private UserService userService;

    private RoleService roleService;

    private TopicService topicService;

    private UiTopicService uiTopicService;

    private QueueService queueService;

    private DbNodeService dbNodeService;

    private Message01Service message01Service;

    private SoaConfig soaConfig;

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private ThreadPoolExecutor executor = null;
    private volatile boolean isRunning = true;
    private AtomicReference<List<QueueOffsetVo>> queueOffsetVos = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<Map<String, List<QueueOffsetVo>>> usingConsumerGroups = new AtomicReference<>(
            new ConcurrentHashMap<>());
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private volatile long lastUpdateTime = 0;

    public UiQueueOffsetService(QueueOffsetService queueOffsetService,
                                AuditLogService auditLogService,
                                ConsumerGroupService consumerGroupService,
                                ConsumerGroupTopicService consumerGroupTopicService,
                                UserInfoHolder userInfoHolder,
                                UserService userService,
                                RoleService roleService,
                                TopicService topicService,
                                UiTopicService uiTopicService,
                                QueueService queueService,
                                DbNodeService dbNodeService,
                                Message01Service message01Service,
                                SoaConfig soaConfig) {
        this.queueOffsetService = queueOffsetService;
        this.auditLogService = auditLogService;
        this.consumerGroupService = consumerGroupService;
        this.consumerGroupTopicService = consumerGroupTopicService;
        this.userInfoHolder = userInfoHolder;
        this.userService = userService;
        this.roleService = roleService;
        this.topicService = topicService;
        this.uiTopicService = uiTopicService;
        this.queueService = queueService;
        this.dbNodeService = dbNodeService;
        this.message01Service = message01Service;
        this.soaConfig = soaConfig;
    }

    public void deleteByQueueId(long queueId, Long topicId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueOffsetEntity.FdQueueId, queueId);
        List<QueueOffsetEntity> queueOffsetEntityList = queueOffsetService.getList(conditionMap);
        List<Long> ids = queueOffsetEntityList.stream().map(QueueOffsetEntity::getId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(ids)){
            queueOffsetService.delete(ids);
            auditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                    "移除queue[" + queueId + "]时，删除queueoffset, " + JsonUtil.toJson(queueOffsetEntityList));        }
    }

    public void createQueueOffsetForExpand(QueueEntity queueEntity, Long topicId, TopicEntity topicEntity) {
        Map<String, Object> conditionMap = new HashMap<>();
        Map<String,ConsumerGroupEntity> consumerGroupMap=consumerGroupService.getCache();
        conditionMap.put(ConsumerGroupTopicEntity.FdTopicId, topicId);
        List<ConsumerGroupTopicEntity> consumerGroupTopicServiceList = consumerGroupTopicService.getList(conditionMap);
        List<QueueOffsetEntity> queueOffsetEntityList = new ArrayList<>();
        consumerGroupTopicServiceList.forEach(consumerGroupTopicEntity -> {
            QueueOffsetEntity queueOffsetEntity = new QueueOffsetEntity();
            queueOffsetEntity.setConsumerGroupId(consumerGroupTopicEntity.getConsumerGroupId());
            queueOffsetEntity.setConsumerGroupName(consumerGroupTopicEntity.getConsumerGroupName());
            //为queueOffset添加origin_consumer_group_name字段
            queueOffsetEntity.setOriginConsumerGroupName(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getOriginName());
            queueOffsetEntity.setConsumerGroupMode(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getMode());
            queueOffsetEntity.setTopicId(topicId);
            queueOffsetEntity.setTopicName(consumerGroupTopicEntity.getTopicName());
            queueOffsetEntity.setOriginTopicName(consumerGroupTopicEntity.getOriginTopicName());
            queueOffsetEntity.setTopicType(consumerGroupTopicEntity.getTopicType());
            queueOffsetEntity.setQueueId(queueEntity.getId());
            queueOffsetEntity.setSubEnv(consumerGroupMap.get(consumerGroupTopicEntity.getConsumerGroupName()).getSubEnv());
            queueOffsetEntity
                    .setDbInfo(queueEntity.getIp() + " | " + queueEntity.getDbName() + " | " + queueEntity.getTbName());
            String userId = userInfoHolder.getUserId();
            queueOffsetEntity.setInsertBy(userId);
            queueOffsetEntityList.add(queueOffsetEntity);
        });
        queueOffsetService.insertBatch(queueOffsetEntityList);
        // 扩容时，把扩容信息同步到 mq2
        //synService32.synTopicExpand(topicEntity, queueEntity, queueOffsetEntityList);

    }

    public List<QueueOffsetEntity> findByQueueId(Long queueId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueOffsetEntity.FdQueueId, queueId);
        return queueOffsetService.getList(conditionMap);
    }

    /**
     * 判断负责人是否存在
     * @param ownerIds
     * @return
     */
    public boolean isOwnerAvailable(List<String> ownerIds){
        List<UserInfo> users=userService.findByUserIds(ownerIds);
        return !CollectionUtils.isEmpty(users);
    }

    public long getUselessConsumerGroupNum() {
        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        return consumerGroupMap.size() - usingConsumerGroups.get().size();
    }

    public long getConsumerGroupNum() {
        return consumerGroupService.getCache().size();
    }

    public long getUsingConsumerGroupNum() {
        return usingConsumerGroups.get().size();
    }
    private volatile long lastAccessTime = System.currentTimeMillis() * 2;

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            lastUpdateTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
            executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    SoaThreadFactory.create("UiQueueOffsetService", true),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            executor.execute(() -> {
                while (isRunning) {
                    try {
                        if (System.currentTimeMillis() - lastAccessTime < soaConfig.getMqReportInterval()
                                || System.currentTimeMillis() - lastAccessTime > 1000 * 60 * 60 * 60) {
                            if (System.currentTimeMillis() - lastUpdateTime > soaConfig.getMqReportInterval()) {
                                initCache();
                                if (queueOffsetVos.get().size() > 0) {
                                    lastUpdateTime = System.currentTimeMillis();
                                    lastAccessTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        log.error("UiQueueOffsetServiceImpl_initCache_error", e);
                    }
                    if (queueOffsetVos.get().size() == 0) {
                        Util.sleep(10 * 1000);
                    } else {
                        Util.sleep(1000);
                    }
                }
            });
        }
    }

    private boolean initCache() {
        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        Map<Long, QueueEntity> queueMap = queueService.getAllQueueMap();
        Map<String, ConsumerGroupTopicEntity> consumerGroupTopicMap = consumerGroupTopicService.getGroupTopic();
        Map<String, TopicEntity> topicMap = topicService.getCache();
        Map<Long, Long> queueMaxIdMap = queueService.getMax();
        List<QueueOffsetEntity> queueOffsetList = queueOffsetService.getCacheData();
        if (consumerGroupMap.size() == 0 || consumerGroupTopicMap.size() == 0 || topicMap.size() == 0
                || queueMaxIdMap.size() == 0 || queueOffsetList.size() == 0) {
            return false;
        }

        List<QueueOffsetVo> queueOffsetVoList = new LinkedList<>();
        Map<String, List<QueueOffsetVo>> usingConsumerGroupMap = new ConcurrentHashMap<>();

        boolean flag = true;

        for (QueueOffsetEntity queueOffsetEntity : queueOffsetList) {

        }


    }

    @Override
    @PreDestroy
    public void stop() {
        isRunning = false;
    }

    @Override
    public String info() {
        return null;
    }
}
