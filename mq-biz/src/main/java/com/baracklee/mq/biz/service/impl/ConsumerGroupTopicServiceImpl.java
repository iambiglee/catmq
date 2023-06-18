package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupTopicRepository;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupTopicCreateResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.CacheUpdateHelper;
import com.baracklee.mq.biz.service.common.MqReadMap;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ConsumerGroupTopicServiceImpl
        extends AbstractBaseService<ConsumerGroupTopicEntity>
        implements ConsumerGroupTopicService , CacheUpdateService, TimerService {
    protected AtomicReference<Map<Long, Map<String, ConsumerGroupTopicEntity>>> consumerGroupTopicRefMap = new AtomicReference<>(
            new HashMap<>());

    protected AtomicReference<Map<String, ConsumerGroupTopicEntity>> groupTopicRefMap = new AtomicReference<>(
            new HashMap<>());

    protected AtomicReference<Map<String, List<ConsumerGroupTopicEntity>>> topicSubscribeRefMap = new AtomicReference<>(
            new HashMap<>());

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private Lock cacheLock = new ReentrantLock();
    protected AtomicBoolean first = new AtomicBoolean(true);
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private AtomicBoolean startFlag = new AtomicBoolean(false);


    @PostConstruct
    public void init(){
        super.setBaseRepository(consumerGroupTopicRepository);
    }

    @Resource
    SoaConfig soaConfig;
    @Resource
    private ConsumerGroupTopicRepository consumerGroupTopicRepository;
    @Resource
    private UserInfoHolder userInfoHolder;
    @Resource
    private RoleService roleService;
    //第一级key为consumergroupid，第二级key为topic名称，value为ConsumerGroupTopicEntity
    @Resource
    private ConsumerGroupService consumerGroupService;
    @Resource
    private TopicService topicService;
    @Resource
    private QueueOffsetService queueOffsetService;
    @Resource
    private AuditLogService auditLogService;
    protected volatile boolean isRunning = true;
    @Override
    public Map<Long, Map<String, ConsumerGroupTopicEntity>> getCache() {
        Map<Long, Map<String, ConsumerGroupTopicEntity>> rs = consumerGroupTopicRefMap.get();
        if(rs.size()==0){
            cacheLock.lock();
            try {
                rs = consumerGroupTopicRefMap.get();
                if(rs.size()==0){
                    if(first.compareAndSet(true,false)){
                        updateCache();
                    }
                    rs=consumerGroupTopicRefMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;
    }

    @Override
    public void deleteByConsumerGroupId(long consumerGroupId) {
        consumerGroupTopicRepository.deleteByConsumerGroupId(consumerGroupId);
        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME,consumerGroupId,
                "取消ConsumerGroup 下所有的topic订阅，删除的consumer和topic 的匹配关系");
    }

    @Override
    public void deleteByOriginTopicName(long consumerGroupId, String originTopicName) {
        consumerGroupTopicRepository.deleteByOriginTopicName(consumerGroupId,originTopicName);
    }

    @Override
    public ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest, Map<String, ConsumerGroupEntity> consumerGroupMap) {
        ConsumerGroupEntity consumerGroupEntity =
                consumerGroupMap.get(consumerGroupTopicCreateRequest.getConsumerGroupName());
        if(consumerGroupEntity.getMode()==2&&consumerGroupEntity.getOriginName().equals(consumerGroupEntity.getName())){
            createConsumerGroupTopicByOrigin(consumerGroupTopicCreateRequest,consumerGroupMap);
        }
        return createConsumerGroupTopicAndFailTopic(consumerGroupTopicCreateRequest, consumerGroupMap);
    }

    /**
     * 删除消费者组，包括关联的数据queue等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumerGroupTopicDeleteResponse deleteConsumerGroupTopic(long consumerGroupTopicId) {
        CacheUpdateHelper.updateCache();
        ConsumerGroupTopicDeleteResponse response = new ConsumerGroupTopicDeleteResponse();
        response.setCode("0");

        try {
            ConsumerGroupTopicEntity consumerGroupTopicEntity = consumerGroupTopicRepository.getById(consumerGroupTopicId);
            if (consumerGroupTopicEntity==null){
                return new ConsumerGroupTopicDeleteResponse();
            }

            Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
            String name=consumerGroupTopicEntity.getConsumerGroupName();

            ConsumerGroupEntity consumerGroupEntity = cache.get(name);
            if (roleService.getRole(userInfoHolder.getUserId(), consumerGroupEntity.getOwnerIds()) >= UserRoleEnum.USER
                    .getRoleCode()) {
                response.setMsg("没有操作权限");
                return response;
            }

            //如果是广播模式，并且是原始消费者组，原始消费者组下面的所有镜像取消
            if(consumerGroupEntity.getMode()==2
                    &&consumerGroupEntity.getOriginName().equals(consumerGroupEntity.getName())){
                deleteByOrigin(cache,consumerGroupEntity,consumerGroupTopicEntity);
            }

            doDelete(consumerGroupTopicEntity);
            CacheUpdateHelper.updateCache();
        } catch (Exception e) {
            response.setCode("1");
            response.setMsg(e.getMessage());
        }
        return response;
    }

    private void deleteByOrigin(Map<String, ConsumerGroupEntity> cache,
                                ConsumerGroupEntity consumerGroupEntity,
                                ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        Map<String, List<ConsumerGroupTopicEntity>> topicSubscribeMap = getTopicSubscribeMap();

        List<ConsumerGroupTopicEntity> consumerGroupTopicEntities =
                topicSubscribeMap.get(consumerGroupTopicEntity.getOriginTopicName());

        for (ConsumerGroupTopicEntity groupTopicEntity : consumerGroupTopicEntities) {
            //如果是镜像消费者组
            if (cache.get(groupTopicEntity.getConsumerGroupName()).getOriginName().equals(consumerGroupEntity.getName())){
                //排除原始组，取消订阅
                if (groupTopicEntity.getId()!=consumerGroupTopicEntity.getId()){
                    doDelete(groupTopicEntity);
                }
            }
        }
    }

    /**
     * 删除customer Group 和关联的所有数据
     * @param groupTopicEntity
     */
    public void doDelete(ConsumerGroupTopicEntity groupTopicEntity) {
        List<String> failTopicNames = new ArrayList<>();
        // 删除失败topic，并且清理失败消息并且解绑失败topic
        String failTopicName= String.format("%s_%s_fail", groupTopicEntity.getConsumerGroupName(), groupTopicEntity.getOriginTopicName());
        failTopicNames.add(failTopicName);
        try {
            topicService.deleteFailTopic(failTopicNames,groupTopicEntity.getConsumerGroupId());
        } catch (Exception e) {
            throw new RuntimeException("操作失败请重试");
        }

        try {
            // 清除topic和失败topic的queueOffset
            queueOffsetService.deleteByConsumerGroupIdAndOriginTopicName(groupTopicEntity);
        } catch (Exception e) {
            throw new RuntimeException("操作失败请重试");
        }

        try {
            // 更新consumerGroup中的topic字段
            consumerGroupService.deleteTopicNameFromConsumerGroup(groupTopicEntity);
        } catch (Exception e) {
            throw new RuntimeException("操作失败，请重试");
        }

        try {
            // 删除正常topic和失败topic的consumerGroupTopic
            deleteByOriginTopicName(groupTopicEntity.getConsumerGroupId(),groupTopicEntity.getOriginTopicName());
        } catch (Exception e) {
            throw new RuntimeException("操作失败，请重试");
        }

        try {
            auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME,
                    groupTopicEntity.getConsumerGroupId(),
                    "取消consumerGroup：" + groupTopicEntity.getConsumerGroupName() + "对主题："
                            + groupTopicEntity.getOriginTopicName() + "的订阅"
                            + JsonUtil.toJson(groupTopicEntity));
            consumerGroupService.notifyMeta(groupTopicEntity.getConsumerGroupId());
            consumerGroupService.notifyRb(groupTopicEntity.getConsumerGroupId());
        } catch (Exception e) {
            throw new RuntimeException("操作失败，请重试");
        }
    }

    @Override
    public Map<String, ConsumerGroupTopicEntity> getGroupTopic() {
        Map<String, ConsumerGroupTopicEntity> rs = groupTopicRefMap.get();
        if (rs.size() == 0) {
            cacheLock.lock();
            try {
                rs = groupTopicRefMap.get();
                if (rs.size() == 0) {
                    if (first.compareAndSet(true, false)) {
                        updateCache();
                    }
                    rs = groupTopicRefMap.get();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return rs;    }

    @Override
    public List<String> getFailTopicNames(long id) {
        return consumerGroupTopicRepository.getFailTopicNames(id);
    }

    @Override
    public ConsumerGroupTopicEntity getCorrespondConsumerGroupTopic(Map<String, Object> parameterMap) {
        return consumerGroupTopicRepository.getCorrespondConsumerGroupTopic(parameterMap);
    }

    private void createConsumerGroupTopicByOrigin(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest, Map<String, ConsumerGroupEntity> consumerGroupMap) {
        for (String key : consumerGroupMap.keySet()) {
            //原始消费者组的订阅不在此处实现
            if(consumerGroupMap.get(key)!=null){
                if(consumerGroupMap.get(key).getOriginName().equals(consumerGroupTopicCreateRequest.getConsumerGroupName())&&
                        consumerGroupMap.get(key).getId()!= consumerGroupTopicCreateRequest.getConsumerGroupId()){
                    ConsumerGroupTopicCreateRequest request=new ConsumerGroupTopicCreateRequest();
                    request.setConsumerGroupName(consumerGroupMap.get(key).getName());
                    request.setConsumerGroupId(consumerGroupMap.get(key).getId());
                    request.setTopicId(consumerGroupTopicCreateRequest.getTopicId());
                    request.setTopicName(consumerGroupTopicCreateRequest.getTopicName());
                    request.setOriginTopicName(consumerGroupTopicCreateRequest.getOriginTopicName());
                    request.setTopicType(consumerGroupTopicCreateRequest.getTopicType());
                    request.setRetryCount(consumerGroupTopicCreateRequest.getRetryCount());
                    request.setThreadSize(consumerGroupTopicCreateRequest.getThreadSize());
                    request.setMaxLag(consumerGroupTopicCreateRequest.getMaxLag());
                    request.setTag(consumerGroupTopicCreateRequest.getTag());
                    request.setDelayProcessTime(consumerGroupTopicCreateRequest.getDelayProcessTime());
                    request.setPullBatchSize(consumerGroupTopicCreateRequest.getPullBatchSize());
                    request.setAlarmEmails(consumerGroupTopicCreateRequest.getAlarmEmails());
                    request.setDelayPullTime(consumerGroupTopicCreateRequest.getDelayPullTime());
                    request.setTimeOut(consumerGroupTopicCreateRequest.getTimeOut());
                    request.setConsumerBatchSize(consumerGroupTopicCreateRequest.getConsumerBatchSize());
                    createConsumerGroupTopicAndFailTopic(request,consumerGroupMap);
                }
            }
        }
    }

    /**
     * 返回创建失败的topic
     * @TODO queueOffsetService 和consuemrgroupservice 基本没有写
     * @param consumerGroupTopicCreateRequest 进入的msg
     * @param consumerGroupMap 失败组
     * @return 失败Group
     */
    public ConsumerGroupTopicCreateResponse createConsumerGroupTopicAndFailTopic(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest, Map<String, ConsumerGroupEntity> consumerGroupMap) {
        CacheUpdateHelper.updateCache();
        if (roleService.getRole(userInfoHolder.getUserId(), consumerGroupMap
                .get(consumerGroupTopicCreateRequest.getConsumerGroupName()).getOwnerIds()) >= UserRoleEnum.USER
                .getRoleCode()) {
            throw new RuntimeException(userInfoHolder.getUserId() + "没有操作权限，请进行权限检查。");
        }
        if (StringUtils.isEmpty(consumerGroupTopicCreateRequest.getTopicName())) {
            return new ConsumerGroupTopicCreateResponse("1", "主题不能为空");
        }
        ConsumerGroupTopicEntity consumerGroupTopicEntity = createConsumerGroupTopic(consumerGroupTopicCreateRequest);
        ConsumerGroupEntity consumerGroupEntity = consumerGroupService
                .get(consumerGroupTopicCreateRequest.getConsumerGroupId());
        TopicEntity topicEntity = topicService.get(consumerGroupTopicCreateRequest.getTopicId());
        TopicEntity failTopicEntity = topicService.createFailTopic(topicEntity, consumerGroupEntity);
        // 创建失败topic的consumerGroupTopic
        consumerGroupTopicCreateRequest.setTopicId(failTopicEntity.getId());
        consumerGroupTopicCreateRequest.setTopicName(failTopicEntity.getName());
        consumerGroupTopicCreateRequest.setTopicType(failTopicEntity.getTopicType());
        ConsumerGroupTopicEntity failConsumerGroupTopicEntity = createConsumerGroupTopic(
                consumerGroupTopicCreateRequest);
        ConsumerGroupTopicCreateResponse consumerGroupTopicCreateResponse=new ConsumerGroupTopicCreateResponse();
        try{
            // 创建正常topic对应的queueOffset
            queueOffsetService.createQueueOffset(consumerGroupTopicEntity);
            // 创建失败topic的queueOffset
            queueOffsetService.createQueueOffset(failConsumerGroupTopicEntity);
            consumerGroupService.addTopicNameToConsumerGroup(consumerGroupTopicEntity);
            consumerGroupService.notifyMeta(consumerGroupTopicCreateRequest.getConsumerGroupId());
            consumerGroupService.notifyRb(consumerGroupTopicCreateRequest.getConsumerGroupId());
        }catch (Exception e){
            consumerGroupTopicCreateResponse.setMsg(e.getMessage());
            consumerGroupTopicCreateResponse.setCode("1");
            throw new RuntimeException(e);
        }
        return consumerGroupTopicCreateResponse;
    }

    public ConsumerGroupTopicEntity createConsumerGroupTopic(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest) {
        ConsumerGroupTopicEntity consumerGroupTopicEntity = new ConsumerGroupTopicEntity();
        consumerGroupTopicEntity.setConsumerGroupId(consumerGroupTopicCreateRequest.getConsumerGroupId());
        consumerGroupTopicEntity.setConsumerGroupName(consumerGroupTopicCreateRequest.getConsumerGroupName());
        consumerGroupTopicEntity.setTopicId(consumerGroupTopicCreateRequest.getTopicId());
        consumerGroupTopicEntity.setTopicName(consumerGroupTopicCreateRequest.getTopicName());
        consumerGroupTopicEntity.setOriginTopicName(consumerGroupTopicCreateRequest.getOriginTopicName());
        consumerGroupTopicEntity.setTopicType(consumerGroupTopicCreateRequest.getTopicType());
        consumerGroupTopicEntity.setMaxPullTime(consumerGroupTopicCreateRequest.getDelayPullTime());
        consumerGroupTopicEntity.setTimeOut(consumerGroupTopicCreateRequest.getTimeOut());
        if (consumerGroupTopicCreateRequest.getRetryCount() != null) {
            consumerGroupTopicEntity.setRetryCount(consumerGroupTopicCreateRequest.getRetryCount());
        }
        if (consumerGroupTopicCreateRequest.getThreadSize() != null) {
            consumerGroupTopicEntity.setThreadSize(consumerGroupTopicCreateRequest.getThreadSize());
        }
        if (consumerGroupTopicCreateRequest.getMaxLag() != null) {
            consumerGroupTopicEntity.setMaxLag(consumerGroupTopicCreateRequest.getMaxLag());
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(consumerGroupTopicCreateRequest.getTag())) {
            consumerGroupTopicEntity.setTag(consumerGroupTopicCreateRequest.getTag());
        }
        if (consumerGroupTopicCreateRequest.getDelayProcessTime() != null) {
            consumerGroupTopicEntity.setDelayProcessTime(consumerGroupTopicCreateRequest.getDelayProcessTime());
        }
        if (consumerGroupTopicCreateRequest.getPullBatchSize() != null) {
            consumerGroupTopicEntity.setPullBatchSize(consumerGroupTopicCreateRequest.getPullBatchSize());
        }
        consumerGroupTopicEntity.setConsumerBatchSize(consumerGroupTopicCreateRequest.getConsumerBatchSize());
        String userId = userInfoHolder.getUserId();
        consumerGroupTopicEntity.setInsertBy(userId);
        consumerGroupTopicEntity.setAlarmEmails(consumerGroupTopicCreateRequest.getAlarmEmails());
        Map<String,ConsumerGroupTopicEntity> groupTopicMap = getGroupTopic();
        if (groupTopicMap.containsKey(consumerGroupTopicCreateRequest.getConsumerGroupName() + "_"
                + consumerGroupTopicCreateRequest.getTopicName())) {
            return groupTopicMap.get(consumerGroupTopicCreateRequest.getConsumerGroupName() + "_"
                    + consumerGroupTopicCreateRequest.getTopicName());
        } else {
            insert(consumerGroupTopicEntity);
            return consumerGroupTopicEntity;
        }
    }

    @Override
    public Map<String, List<ConsumerGroupTopicEntity>> getTopicSubscribeMap() {
        return topicSubscribeRefMap.get();
    }

    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    @Override
    public void updateCache() {
        if(updateFlag.compareAndSet(false,true)){
            if(checkChanged()){
                forceUpdateCache();
            }
            updateFlag.set(false);
        }
    }

    @Override
    public void updateEmailByGroupName(String groupName, String alarmEmails) {
        consumerGroupTopicRepository.updateEmailByGroupName(groupName,alarmEmails);
    }

    @Override
    public ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest) {
        return subscribe(consumerGroupTopicCreateRequest,consumerGroupService.getCache());
    }

    protected volatile LastUpdateEntity lastUpdateEntity = null;
    protected long lastTime=System.currentTimeMillis();
    protected boolean checkChanged() {
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
        LastUpdateEntity temp = consumerGroupTopicRepository.getLastUpdate();
        if((lastUpdateEntity==null&&temp!=null)||(lastUpdateEntity!=null||temp==null)){
            lastUpdateEntity=temp;
            flag=true;
        }else if (lastUpdateEntity!=null&&temp!=null&& (
                lastUpdateEntity.getMaxId()!=temp.getMaxId()
                        ||temp.getLastDate().getTime()!=lastUpdateEntity.getLastDate().getTime()
                        ||temp.getCount() != lastUpdateEntity.getCount()
                )){
            lastUpdateEntity=temp;
            flag=true;
        }
        if(!flag&&consumerGroupTopicRefMap.get().size()==0){
            log.warn("consumerGroupTopic数据为空，请注意！");
            return true;}
        return flag;
    }

    @Override
    public void forceUpdateCache() {
        List<ConsumerGroupTopicEntity> consumerGroupEntities = getList();
        MqReadMap<Long, Map<String, ConsumerGroupTopicEntity>> dataMap = new MqReadMap<>(consumerGroupEntities.size()/3);
        MqReadMap<String, ConsumerGroupTopicEntity> groupTopicMap = new MqReadMap<>(consumerGroupEntities.size());
        MqReadMap<String, List<ConsumerGroupTopicEntity>> topicSubscribeMap = new MqReadMap<>(consumerGroupEntities.size());
        for (ConsumerGroupTopicEntity t1 : consumerGroupEntities) {
            if(!dataMap.containsKey(t1.getConsumerGroupId())){
                dataMap.put(t1.getConsumerGroupId(),new HashMap<>());
            }
            dataMap.get(t1.getConsumerGroupId()).put(t1.getTopicName(),t1);
            if(!groupTopicMap.containsKey(t1.getConsumerGroupName()+"_"+t1.getTopicName())){
                groupTopicMap.put(t1.getConsumerGroupName() + "_" + t1.getTopicName(), t1);
            }
            if(!topicSubscribeMap.containsKey(t1.getTopicName())){
                topicSubscribeMap.put(t1.getTopicName(),new ArrayList<>());
            }
            topicSubscribeMap.get(t1.getTopicName()).add(t1);
        }
        dataMap.setOnlyRead();
        groupTopicMap.setOnlyRead();
        consumerGroupTopicRefMap.set(dataMap);
        groupTopicRefMap.set(groupTopicMap);
        topicSubscribeRefMap.set(topicSubscribeMap);
    }

    @Override
    public String getCacheJson() {
        return JsonUtil.toJson(getCache());
    }

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            updateCache();
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    while (isRunning) {
                        try {
                            updateCache();
                        } catch (Throwable e) {
                            log.error("ConsumerGroupTopicService_updateCache_error", e);
                        }
                        Util.sleep(soaConfig.getMqConsumerGroupTopicCacheInterval());
                    }
                }
            });
        }
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public String info() {
        return null;
    }
}
