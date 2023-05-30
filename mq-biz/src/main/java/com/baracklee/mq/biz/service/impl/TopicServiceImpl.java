package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.TopicRepository;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends AbstractBaseService<TopicEntity> implements CacheUpdateService, TopicService, TimerService {

    private Logger log = LoggerFactory.getLogger(TopicServiceImpl.class);
    @Resource
    private TopicRepository topicRepository;
    @Resource
    private SoaConfig soaConfig;
    @Resource
    private ConsumerGroupTopicService consumerGroupTopicService;
    @Resource
    private AuditLogService uiAuditLogService;
    @Resource
    private UserInfoHolder userInfoHolder;
    @Resource
    QueueService queueService;
    @Resource
    Message01Service message01Service;

    private volatile boolean isRunning = true;

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("TopicService", true),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private AtomicReference<Map<String, TopicEntity>> topicCacheMapRef = new AtomicReference<>(
            new ConcurrentHashMap<>(2000));


    @PostConstruct
    private void init() {
        super.setBaseRepository(topicRepository);
        // System.out.println("topic init");
    }

    @Override
    public TopicEntity getTopicByName(String topicName) {
        return topicRepository.getTopicByName(topicName);
    }

    @Override
    public List<TopicEntity> getListWithUserName(Map<String, Object> conditionMap, long page, long pageSize) {
        conditionMap.put("start1",(page-1)*pageSize);
        conditionMap.put("offset1", pageSize);
        return topicRepository.getListWithUserName(conditionMap);
    }

    @Override
    public TopicEntity createFailTopic(TopicEntity topicEntity, ConsumerGroupEntity consumerGroup) {
        TopicEntity failTopicEntity = new TopicEntity();
        failTopicEntity.setName(String.format("%s_%s_fail", consumerGroup.getName(), topicEntity.getName()));
        failTopicEntity.setOriginName(topicEntity.getOriginName());
        failTopicEntity.setDptName(topicEntity.getDptName());
        failTopicEntity.setOwnerIds(consumerGroup.getOwnerIds());
        failTopicEntity.setOwnerNames(consumerGroup.getOwnerNames());
        failTopicEntity.setEmails(consumerGroup.getAlarmEmails());
        failTopicEntity.setTels(topicEntity.getTels());
        failTopicEntity.setBusinessType(topicEntity.getBusinessType());
        failTopicEntity.setRemark(topicEntity.getRemark());
        failTopicEntity.setToken(topicEntity.getToken());
        failTopicEntity.setNormalFlag(topicEntity.getNormalFlag());
        failTopicEntity.setTopicType(2);
        failTopicEntity.setConsumerFlag(topicEntity.getConsumerFlag());
        failTopicEntity.setConsumerGroupNames(topicEntity.getConsumerGroupNames());
        failTopicEntity.setAppId(consumerGroup.getAppId());
        String userId = userInfoHolder.getUserId();
        failTopicEntity.setInsertBy(userId);
        if (getCache().containsKey(failTopicEntity.getName())) {
            return getCache().get(failTopicEntity.getName());
        } else {
            insert(failTopicEntity);
            distributeQueueWithLock(failTopicEntity, 2, 2);
            return getTopicByName(failTopicEntity.getName());
        }
    }

    @Override
    public void distributeQueueWithLock(TopicEntity topicEntity, int queueNum, int nodeType) {
        log.info("distributeQueueWithLock start; topicId: " + topicEntity.getId() + "; queueNum: " + queueNum);

        int unselectedSize=queueNum;

            while (unselectedSize>0){
                List<QueueEntity> topUndistributed = queueService.getTopUndistributed(unselectedSize, nodeType, topicEntity.getId());
                if (CollectionUtils.isEmpty(topUndistributed)){
                    uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(), "数据节点不够分配");
                    return;
                }
                distributeQueue(topicEntity,topUndistributed);
                unselectedSize=topUndistributed.size();
            }
    }

    /**
     * 插入时间范围之内的数据
     */
    @Override
    public long getMsgCount(String topicName, String start, String end) {
        List<QueueEntity> data= queueService.getAllLocatedTopicQueue().get(topicName);
        long count=0;
        if(data!=null&&start!=null&&end!=null){
            for (QueueEntity entity : data) {
                message01Service.setDbId(entity.getDbNodeId());
                List<Message01Entity> msgs = message01Service.getListByTime(entity.getTbName(), start);
                if(!CollectionUtils.isEmpty(msgs)) {
                    count=count-msgs.get(0).getId();
                }
                message01Service.setDbId(entity.getDbNodeId());
                msgs=message01Service.getListByTime(entity.getTbName(),end);
                if(!CollectionUtils.isEmpty(msgs)) {
                    count=count+msgs.get(0).getId();
                }
                Util.sleep(10);
            }
        }
        return count;
    }

    @Override
    public void deleteFailTopic(List<String> failTopicNames, long id) {
        if (CollectionUtils.isEmpty(failTopicNames)) return;
        for (String failTopicName : failTopicNames) {
            TopicEntity topicByName = getTopicByName(failTopicName);
            if(topicByName!=null){
                List<QueueEntity> queueEntities = queueService.getQueuesByTopicId(topicByName.getId());
                queueService.deleteMessage(queueEntities,id);
                delete(topicByName.getId());
                log.warn("consumer_group_{},删除失败topic{}",id, JsonUtil.toJson(topicByName));
            }
        }
    }

    @Override
    public Map<String, TopicEntity> getCache() {
        List<TopicEntity> topicEntities = topicRepository.getAll();
        MqReadMap<String, TopicEntity> topicCacheMap = new MqReadMap<String, TopicEntity>(topicEntities.size());
        for (TopicEntity topicEntity : topicEntities) {
            topicCacheMap.put(topicEntity.getName(),topicEntity);
        }
        topicCacheMap.setOnlyRead();
        return topicCacheMap;
    }

    @Override
    public void distributeQueue(TopicEntity normalTopicEntity, QueueEntity queueEntity) {
        queueEntity.setTopicName(normalTopicEntity.getName());
        queueEntity.setTopicId(normalTopicEntity.getId());
        queueEntity.setReadOnly(1);
        queueService.updateWithLock(queueEntity);    }

    @Override
    public void distributeQueue(TopicEntity topicEntity, List<QueueEntity> queueEntityList) {
        for (QueueEntity queueEntity : queueEntityList) {
            distributeQueue(topicEntity,queueEntity);
        }
    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            updateCache();
            executor.execute(() -> {
                while (isRunning) {
                    updateCache();
                    Util.sleep(soaConfig.getMqTopicCacheInterval());
                }
            });
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        isRunning=false;
    }

    @Override
    public String info() {
        return null;
    }

    @PreDestroy
    private void close() {
        isRunning = false;
        try {
            executor.shutdown();
        } catch (Throwable e) {
            // TODO: handle exception
        }
    }

    @Override
    public void updateCache() {
        if (updateFlag.compareAndSet(false,true)){
            if(checkChange()){
                forceUpdateCache();
            }
            updateFlag.set(false);
        }
    }

    private volatile LastUpdateEntity lastUpdateEntity = null;
    private long lastTime=System.currentTimeMillis();
    private boolean checkChange() {
        boolean flag= doCheckChanged();
        if(!flag){
            if(System.currentTimeMillis()-lastTime>soaConfig.getMqMetaRebuildMaxInterval()){
                lastTime=System.currentTimeMillis();
                return true;
            }
        }else{
            lastTime=System.currentTimeMillis();
        }
        return flag;
    }

    private boolean doCheckChanged() {
        boolean flag=false;
        LastUpdateEntity temp = topicRepository.getLastUpdate();
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
        if(!flag && topicCacheMapRef.get().size()==0){
            log.warn("Topic数据为空，请注意！");
            return true;
        }
        return flag;
    }

    @Override
    public long countWithUserName(Map<String, Object> conditionMap) {
        return topicRepository.countWithUserName(conditionMap);
    }

    /**
     * 如果consumerGroup 被修改了, 这里也要对应修改失败队列的信息
     */
    @Override
    public void updateFailTopic(ConsumerGroupEntity consumerGroupEntity) {
        List<String> failTopicNames = consumerGroupTopicService.getFailTopicNames(consumerGroupEntity.getId());
        if (failTopicNames.size()>0){
            for (String failTopicName : failTopicNames) {
                TopicEntity failTopicEntity = getTopicByName(failTopicName);
                if (failTopicEntity != null) {
                    failTopicEntity.setAppId(consumerGroupEntity.getAppId());
                    update(failTopicEntity);
                }
            }
        }
    }

    @Override
    public void forceUpdateCache() {
        doForceUpdateCache();
        updateQueueCache();
    }

    private void updateQueueCache() {
        executor.submit(()->{
            queueService.forceUpdateCache();
        });
    }

    private void doForceUpdateCache() {
        List<TopicEntity> data = topicRepository.getAll();
        if(CollectionUtils.isEmpty(data)) return;
        Map<String, TopicEntity> topicCacheMap = data.stream().collect(Collectors.toMap(TopicEntity::getName, e -> e));
        topicCacheMapRef.set(topicCacheMap);
    }

    @Override
    public String getCacheJson() {
        return JsonUtil.toJsonNull(getCacheJson());
    }
}
