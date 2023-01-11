package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupTopicRepository;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

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

public class ConsumerGroupTopicServiceImpl
        extends AbstractBaseService<ConsumerGroupTopicEntity>
        implements ConsumerGroupTopicService , CacheUpdateService {
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
    public ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest, Map<String, ConsumerGroupEntity> consumerGroupMap) {
        ConsumerGroupEntity consumerGroupEntity =
                consumerGroupMap.get(consumerGroupTopicCreateRequest.getConsumerGroupName());
        if(consumerGroupEntity.getMode()==2&&consumerGroupEntity.getOriginName().equals(consumerGroupEntity.getName())){
            createConsumerGroupTopicByOrigin(consumerGroupTopicCreateRequest,consumerGroupMap);
        }
        return createConsumerGroupTopicAndFailTopic(consumerGroupTopicCreateRequest, consumerGroupMap);
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
    private ConsumerGroupTopicCreateResponse createConsumerGroupTopicAndFailTopic(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest, Map<String, ConsumerGroupEntity> consumerGroupMap) {
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

    private ConsumerGroupTopicEntity createConsumerGroupTopic(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest) {
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

    protected volatile LastUpdateEntity lastUpdateEntity = null;
    protected long lastTime=System.currentTimeMillis();
    protected boolean checkChanged() {
        boolean flag= doCheckChanged();
        if(!flag){
            if(System.currentTimeMillis()-lastTime>soaConfig.getMetaMqRebuildMaxInterval()){
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
        return null;
    }
}
