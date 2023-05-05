package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.entity.Message01Entity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TableInfoEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.ui.dto.response.QueueCountResponse;
import com.baracklee.mq.biz.ui.vo.QueueVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Barack Lee
 */
@Service
public class UiQueueService implements TimerService {

    Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private QueueService queueService;

    private QueueOffsetService queueOffsetService;

    private Message01Service message01Service;

    private DbNodeService dbNodeService;

    private TopicService topicService;

    private AuditLogService auditLogService;

    private UiQueueOffsetService uiQueueOffsetService;

    private UiConsumerGroupTopicService uiConsumerGroupTopicService;

    private ConsumerGroupService consumerGroupService;

    private RoleService roleService;

    private UserInfoHolder userInfoHolder;

    private SoaConfig soaConfig;

    @Autowired
    public UiQueueService(QueueService queueService,
                          QueueOffsetService queueOffsetService,
                          Message01Service message01Service,
                          DbNodeService dbNodeService,
                          TopicService topicService,
                          AuditLogService auditLogService,
                          UiQueueOffsetService uiQueueOffsetService,
                          UiConsumerGroupTopicService uiConsumerGroupTopicService,
                          ConsumerGroupService consumerGroupService,
                          RoleService roleService,
                          UserInfoHolder userInfoHolder,
                          SoaConfig soaConfig) {
        this.queueService = queueService;
        this.queueOffsetService = queueOffsetService;
        this.message01Service = message01Service;
        this.dbNodeService = dbNodeService;
        this.topicService = topicService;
        this.auditLogService = auditLogService;
        this.uiQueueOffsetService = uiQueueOffsetService;
        this.uiConsumerGroupTopicService = uiConsumerGroupTopicService;
        this.consumerGroupService = consumerGroupService;
        this.roleService = roleService;
        this.userInfoHolder = userInfoHolder;
        this.soaConfig=soaConfig;
    }

    private volatile boolean isRunning = true;

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private ThreadPoolExecutor executor = null;

    private AtomicReference<List<QueueVo>> queueListAvg = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<QueueVo>> queueListByDataSize = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<QueueVo>> queueWarningInfo = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<QueueVo>> queueListCount = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<Map<String, List<QueueVo>>> queueListCountMap = new AtomicReference<>(
            new ConcurrentHashMap<>());
    private volatile long messageCount = 0;
    private volatile long messageAvg = 0;
    private volatile long lastUpdateTime = 0;

    private long lastAccessTime = System.currentTimeMillis() * 2;

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            lastUpdateTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
            executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    SoaThreadFactory.create("UiQueueService", true), new ThreadPoolExecutor.DiscardOldestPolicy());
            executor.execute(() -> {
                while (isRunning) {
                    try {
                        if (System.currentTimeMillis() - lastAccessTime < soaConfig.getMqReportInterval()
                                || System.currentTimeMillis() - lastAccessTime > 1000 * 60 * 60 * 60) {
                            if (System.currentTimeMillis() - lastUpdateTime > soaConfig.getMqReportInterval()) {
                                initCache();
                                initMessageCount();
                                if (queueListAvg.get().size() > 0) {
                                    lastUpdateTime = System.currentTimeMillis();
                                    lastAccessTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        log.error("UiQueueServiceImpl_initCache_error", e);
                    }
                    if (queueListAvg.get().size() == 0) {
                        Util.sleep(10 * 1000);
                    } else {
                        Util.sleep(2000);
                    }
                }
            });
        }
    }

    private void initMessageCount() {
        long count =0;
        long avg= 0;

        for (QueueVo queueVo : queueListAvg.get()) {
            count+=queueVo.getMsgCount();
            avg+=queueVo.getAvgCount();
        }
        messageCount=count;
        messageAvg=avg;
    }

    public List<QueueEntity> getBestRemoveQueue(Long topicId) {
        List<Long> nodeIds;
        nodeIds = queueService.getTopDistributedNodes(topicId);
        if (CollectionUtils.isEmpty(nodeIds)) {
            return null;
        }
        for (Long nodeId : nodeIds) {
            if (getQueuesByTopicAndNodeId(topicId, nodeId).size() > 1) {
                return queueService.getDistributedList(Collections.singletonList(nodeId), topicId);
            }
        }
        return null;
    }

    private List<QueueEntity> getQueuesByTopicAndNodeId(Long topicId, Long nodeId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueEntity.FdTopicId, topicId);
        conditionMap.put(QueueEntity.FdDbNodeId, nodeId);
        return queueService.getList(conditionMap);
    }
    public long getMessageCount() {
        return messageCount;
    }

    public long getMessageAvg() {
        return messageAvg;
    }

    private boolean initCache() {
        List<QueueEntity> queueList = queueService.getAllLocatedQueue();
        if (queueList.size()==0){
            return false;
        }
        List<QueueVo> queueVos = new ArrayList<>(queueList.size());
        Map<String, List<QueueVo>> queueVoMap = new ConcurrentHashMap<>();
        Map<Long, Long> queueMaxIdMap = queueService.getMax();
        if(queueMaxIdMap.size()==0){
            return false;
        }

        Map<String, TopicEntity> topicMap = topicService.getCache();
        if (topicMap.size() == 0){
            return false;
        }

        for (QueueEntity queueEntity : queueList) {
            TopicEntity topicEntity = topicMap.get(queueEntity);
            if (topicEntity==null){
                continue;
            }
            QueueVo queueVo = new QueueVo(queueEntity);
            queueVo.setDbReadOnly(dbNodeService.getCache().get(queueEntity.getDbNodeId()).getReadOnly());

            if (!queueMaxIdMap.containsKey(queueEntity.getId())){
                log.info("queueid " + queueEntity.getId() + " not exist!");
                continue;
            }

            long maxId = queueMaxIdMap.get(queueEntity.getId());

            queueVo.setMaxId(maxId);

            //获取倒数第一条信息
            message01Service.setDbId(queueEntity.getDbNodeId());
            Message01Entity message01Entity = message01Service.getMinIdMsg(queueEntity.getTbName());
            TableInfoEntity tableInfo = message01Service.getSingleTableInfoFromCache(queueEntity);
            queueVo.setMsgCount(tableInfo.getTbRows());
            queueVo.setAvgCount(queueVo.getMsgCount()/topicEntity.getSaveDayNum());

            //插入空间table信息
            queueVo.setDataSize(tableInfo.getDataSize());
            if(!StringUtils.isEmpty(topicEntity.getOwnerNames())){
                queueVo.setTopicOwnerName(topicEntity.getOwnerNames());
                queueVo.setSaveDayNum(topicEntity.getSaveDayNum());
            }
            if (message01Entity != null) {
                queueVo.setMinTime(message01Entity.getSendTime());
                if ((System.currentTimeMillis()
                        - message01Entity.getSendTime().getTime()) > (long) (topicEntity.getSaveDayNum() + 1) * 24
                        * 60 * 60 * 1000) {
                    queueVo.setIsException(1);
                }
            }
            queueVos.add(queueVo);
            if (queueVoMap.containsKey(queueVo.getTopicName())) {
                queueVoMap.get(queueVo.getTopicName()).add(queueVo);
            } else {
                List<QueueVo> queueVoList2 = new ArrayList<>();
                queueVoList2.add(queueVo);
                queueVoMap.put(queueVo.getTopicName(), queueVoList2);
            }
        }
        List<QueueVo> queueVosAvg=queueSortAvg(queueVos);
        List<QueueVo> queueVosCount=queueVosCount(queueVos);
        List<QueueVo> queueSortByDataSize=queueSortByDataSize(queueVos);

        queueListAvg.set(queueVosAvg);
        queueListCount.set(queueVosCount);
        queueListCountMap.set(queueVoMap);
        queueListByDataSize.set(queueSortByDataSize);
        return true;
    }

    private List<QueueVo> queueSortByDataSize(List<QueueVo> queueVos) {
        List<QueueVo> queueVos1 = new ArrayList<>(queueVos);
        queueVos1.sort((q1,q2)->{
            if(q1.getDataSize()>q2.getDataSize()){
                return -1;
            }else if(q1.getDataSize()<q2.getDataSize()){
                return 1;
            }
            return 0;
        });
        return queueVos1;
    }

    private List<QueueVo> queueVosCount(List<QueueVo> queueVos) {
        List<QueueVo> queueVos1 = new ArrayList<>(queueVos);
        queueVos1.sort((q1,q2)->{
            if (q1.getMsgCount() > q2.getMsgCount()) {
                return -1;
            } else if (q1.getMsgCount() < q2.getMsgCount()) {
                return 1;
            }
            return 0;
        });
        return queueVos1;
    }

    private List<QueueVo> queueSortAvg(List<QueueVo> queueVos) {
        List<QueueVo> queueVos1 = new ArrayList<>(queueVos);
        queueVos1.sort((q1,q2)->{
            if(q1.getAvgCount()>q2.getAvgCount()){
                return -1;
            }else if(q1.getAvgCount()<q2.getAvgCount()){
                return 1;
            }
            return 0;
        });
        return queueVos1;
    }

    public QueueCountResponse count(int nodeType) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueEntity.FdNodeType, nodeType);
        Long allCount = queueService.count(conditionMap);
        conditionMap.put(QueueEntity.FdTopicId, "0");
        Long undistributedCount = queueService.count(conditionMap);
        Long distributedCount = allCount - undistributedCount;
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put("allCount", allCount);
        resultMap.put("distributedCount", distributedCount);
        resultMap.put("undistributedCount", undistributedCount);

        return new QueueCountResponse(resultMap);
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
