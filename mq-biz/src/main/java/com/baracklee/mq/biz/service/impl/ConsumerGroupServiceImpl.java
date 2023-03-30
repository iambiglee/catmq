package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupRepository;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.dto.request.ConsumerGroupCreateRequest;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupCreateResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupEditResponse;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.AuditUtil;
import com.baracklee.mq.biz.service.common.CacheUpdateHelper;
import com.baracklee.mq.biz.service.common.MessageType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsumerGroupServiceImpl extends AbstractBaseService<ConsumerGroupEntity>
        implements CacheUpdateService, ConsumerGroupService, TimerService {
    @PostConstruct
    protected void init() {
        super.setBaseRepository(consumerGroupRepository);
    }

    private AtomicBoolean updateFlag = new AtomicBoolean(false);
    @Resource
    QueueOffsetService queueOffsetService;
    @Resource
    private ConsumerGroupTopicService consumerGroupTopicService;

    @Resource
    private ConsumerGroupRepository consumerGroupRepository;
    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private RoleService roleService;

    @Resource
    private UserInfoHolder userInfoHolder;

    @Resource
    private ConsumerService consumerService;
    @Resource
    private TopicService topicService;

    protected volatile boolean isRunning = true;
    private AtomicBoolean startFlag = new AtomicBoolean(false);

    @Resource
    AuditLogService auditLogService;

    protected AtomicReference<Map<String, ConsumerGroupEntity>> consumerGroupRefMap = new AtomicReference<>(
            new HashMap<>());
    protected AtomicReference<Map<Long, ConsumerGroupEntity>> consumerGroupByIdRefMap = new AtomicReference<>(
            new HashMap<>());
    private final AtomicReference<List<String>> subEnvList = new AtomicReference<>(new ArrayList<>());


    private Lock cacheLocal=new ReentrantLock();
    @Resource
    private SoaConfig soaConfig;

    private final AtomicBoolean first= new AtomicBoolean(true);
    @Override
    public Map<String, ConsumerGroupEntity> getCache() {
        Map<String, ConsumerGroupEntity> rs = consumerGroupRefMap.get();
        if(rs.size()==0){
            cacheLocal.lock();
            try {
                rs = consumerGroupRefMap.get();
                if (rs.size() == 0) {
                    if (first.compareAndSet(true, false)) {
                        updateCache();
                    }
                    rs = consumerGroupRefMap.get();
                }
                } finally{
                    cacheLocal.unlock();
                }
            }
        return rs;
    }

    @Override
    public Map<String, ConsumerGroupEntity> getData() {
        return null;
    }

    @Override
    public void copyAndNewConsumerGroup(ConsumerGroupEntity consumerGroupEntityOld,
                                        ConsumerGroupEntity consumerGroupEntityNew) {
        consumerGroupEntityNew.setId(0);
        insert(consumerGroupEntityNew);
        Map<String, ConsumerGroupEntity> consumerGroupMap = getConsumerGroupByName(consumerGroupEntityOld.getName());
        Map<Long, Map<String, ConsumerGroupTopicEntity>> ctMap = consumerGroupTopicService.getCache();
        Map<String, ConsumerGroupTopicEntity> consumerTopics = ctMap.get(consumerGroupEntityOld.getId());
        if(consumerTopics!=null){
            for (Map.Entry<String, ConsumerGroupTopicEntity> entry : consumerTopics.entrySet()) {
                if (entry.getValue().getTopicType() == 1) {
                    ConsumerGroupTopicCreateRequest request2 = new ConsumerGroupTopicCreateRequest();
                    request2.setAlarmEmails(entry.getValue().getAlarmEmails());
                    request2.setConsumerBatchSize(entry.getValue().getConsumerBatchSize());
                    request2.setConsumerGroupId(consumerGroupEntityNew.getId());
                    request2.setConsumerGroupName(consumerGroupEntityNew.getName());
                    request2.setDelayProcessTime(entry.getValue().getDelayProcessTime());
                    request2.setDelayPullTime(entry.getValue().getMaxPullTime());
                    request2.setMaxLag(entry.getValue().getMaxLag());
                    request2.setOriginTopicName(entry.getValue().getOriginTopicName());
                    request2.setPullBatchSize(entry.getValue().getPullBatchSize());
                    request2.setRetryCount(entry.getValue().getRetryCount());
                    request2.setTag(entry.getValue().getTag());
                    request2.setThreadSize(entry.getValue().getThreadSize());
                    request2.setTopicId(entry.getValue().getTopicId());
                    request2.setTopicName(entry.getValue().getTopicName());
                    request2.setTopicType(entry.getValue().getTopicType());
                    request2.setTimeOut(entry.getValue().getTimeOut());
                    consumerGroupTopicService.subscribe(request2,consumerGroupMap);
                }
            }
        }
    }

    @Override
    public BaseUiResponse deleteTopicNameFromConsumerGroup(ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        return null;
    }

    private Map<String, ConsumerGroupEntity> getConsumerGroupByName(String name) {
        HashMap<String, Object> conditions = new HashMap<>();
        conditions.put("originName",name);
        List<ConsumerGroupEntity> consumerGroupEntities = getList(conditions);
        Map<String, ConsumerGroupEntity> map=new HashMap<>();
        consumerGroupEntities.forEach(t-> map.put(t.getName(),t));
        return map;
    }

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

    private boolean checkChanged() {
        boolean flag = doCheckChanged();
        if(!flag){
            if(System.currentTimeMillis()-lastTime>soaConfig.getMqMetaRebuildMaxInterval()){
                lastTime=System.currentTimeMillis();
                return true;
            }
        }else {
            lastTime=System.currentTimeMillis();
        }
        return flag;
    }

    private boolean doCheckChanged() {
        boolean flag=false;
        LastUpdateEntity temp = consumerGroupRepository.getLastUpdate();
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
        if (!flag && consumerGroupRefMap.get().size() == 0) {
            return true;
        }
        return flag;
    }

    @Override
    public void forceUpdateCache() {
        List<ConsumerGroupEntity> consumerGroupEntities = getList();
        HashMap<String, ConsumerGroupEntity> dataMap = new HashMap<>(consumerGroupEntities.size());
        HashMap<Long, ConsumerGroupEntity> dataIdMap = new HashMap<>(consumerGroupEntities.size());
        List<String> envList=new ArrayList<>();
        envList.add("default");
        consumerGroupEntities.forEach(t1->{
            dataMap.put(t1.getName(),t1);
            dataIdMap.put(t1.getId(),t1);
            if(!envList.contains(t1.getSubEnv())){
                envList.add(t1.getSubEnv());
            }
        });
        if(dataMap.size()>0&&dataIdMap.size()>0){
            consumerGroupRefMap.set(dataMap);
            consumerGroupByIdRefMap.set(dataIdMap);
            subEnvList.set(envList);
        }else {
            lastUpdateEntity=null;
        }
    }

    @Override
    public Map<String, ConsumerGroupEntity> getByNames(List<String> names) {
        if(CollectionUtils.isEmpty(names)) return new HashMap<>();
        List<ConsumerGroupEntity> consumerGroupEntities = consumerGroupRepository.getByNames(names);
        Map<String, ConsumerGroupEntity> map =
                consumerGroupEntities.stream().collect(Collectors.toMap(ConsumerGroupEntity::getName,
                        Function.identity()));
        return map;
    }

    @Override
    public List<ConsumerGroupTopicEntity> getGroupTopic() {
        return null;
    }

    @Override
    public List<ConsumerGroupEntity> getByOwnerNames(Map<String, Object> parameterMap) {
        return null;
    }

    @Override
    public long countByOwnerNames(Map<String, Object> parameterMap) {
        return 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notifyRb(List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)) return;
        updateRbVersion(ids);
        List<NotifyMessageEntity> notifyMessageEntities = new ArrayList<>();
        for (Long id : ids) {
            NotifyMessageEntity notifyMessageEntity = new NotifyMessageEntity();
            notifyMessageEntity.setConsumerGroupId(id);
            notifyMessageEntity.setMessageType(1);
            notifyMessageEntities.add(notifyMessageEntity);

            notifyMessageEntity = new NotifyMessageEntity();
            notifyMessageEntity.setConsumerGroupId(id);
            notifyMessageEntity.setMessageType(2);
            notifyMessageEntities.add(notifyMessageEntity);
        }
        notifyMessageService.insertBatch(notifyMessageEntities);
    }

    @Override
    public void notifyRbByNames(List<String> consumerGroupNames) {

    }

    @Override
    public void notifyMetaByNames(List<String> consumerGroupNames) {

    }



    @Override
    public void notifyMeta(List<Long> consumerGroupIds) {

    }

    @Override
    public void notifyOffset(long consumerGroupId) {

    }

    @Override
    public List<ConsumerGroupEntity> getLastMetaConsumerGroup(long minMessageId, long maxMessageId) {
        return null;
    }

    @Override
    public List<ConsumerGroupEntity> getLastRbConsumerGroup(long minMessageId, long maxMessageId) {
        return consumerGroupRepository.getLastConsumerGroup(minMessageId, maxMessageId, MessageType.Rb);
    }

    @Override
    public void rb(List<QueueOffsetEntity> queueOffsetEntities) {
        Map<Long, String> idsMap = new HashMap<>(30);
        List<NotifyMessageEntity> notifyMessageEntities = new ArrayList<>(30);
        queueOffsetEntities.forEach(t1->{
            idsMap.put(t1.getConsumerGroupId(), "");
            // 更新consumerid 和consumername
            queueOffsetService.updateConsumerId(t1);
        });
        //保证重平衡版本号
        updateRbVersion(new ArrayList<>(idsMap.keySet()));
        notifyMessageService.insertBatch(notifyMessageEntities);
    }

    @Override
    public void notifyRb(long consumerGroupId) {

    }

    @Override
    public BaseUiResponse<Void> addTopicNameToConsumerGroup(ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        ConsumerGroupEntity consumerGroupEntity = get(consumerGroupTopicEntity.getConsumerGroupId());
        String oldTopicNames = consumerGroupEntity.getTopicNames();

        // 如果mq3的group的订阅关系中，没有该topic则添加
        if (StringUtils.isNotEmpty(oldTopicNames)) {
            List<String> oldTopicNameList = Arrays.asList(oldTopicNames.split(","));
            if (!oldTopicNameList.contains(consumerGroupTopicEntity.getTopicName())) {
                consumerGroupEntity.setTopicNames(oldTopicNames + "," + consumerGroupTopicEntity.getTopicName());
            }
        } else {
            consumerGroupEntity.setTopicNames(consumerGroupTopicEntity.getTopicName());
        }
        update(consumerGroupEntity);
        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, consumerGroupEntity.getId(),
                "新增订阅，修改consumerGroup的topic字段，从" + oldTopicNames + "变为：" + consumerGroupEntity.getTopicNames());

        return new BaseUiResponse<Void>();
    }

    @Override
    public void deleteUnuseBroadConsumerGroup() {

    }

    @Override
    public void notifyMeta(long consumerGroupId) {
        updateMetaVersion(Arrays.asList(consumerGroupId));
        NotifyMessageEntity notifyMessageEntity = new NotifyMessageEntity();
        notifyMessageEntity.setConsumerGroupId(consumerGroupId);
        notifyMessageEntity.setMessageType(MessageType.Meta);
        notifyMessageService.insert(notifyMessageEntity);
    }

    @Override
    public Map<Long, ConsumerGroupEntity> getIdCache() {
        Map<Long, ConsumerGroupEntity> rs = consumerGroupByIdRefMap.get();
        if(rs.size()==0){
            cacheLocal.lock();
            try {
                rs=consumerGroupByIdRefMap.get();
                if (rs.size()==0){
                    if (first.compareAndSet(true,false)){
                        updateCache();
                    }
                    rs=consumerGroupByIdRefMap.get();
                }
            }finally {
                cacheLocal.unlock();
            }
        }
        return rs;
    }

    @Override
    public List<String> getSubEnvList() {
        return null;
    }

    @Override
    public ConsumerGroupDeleteResponse deleteConsumerGroup(long consumerGroupId, boolean checkOnline) {
        forceUpdateCache();
        Map<String, ConsumerGroupEntity> cache = getCache();
        ConsumerGroupEntity consumerGroupEntity = get(consumerGroupId);
        if (consumerGroupEntity.getMode() == 2
                && consumerGroupEntity.getOriginName().equals(consumerGroupEntity.getName())
        ) {
            for ( Map.Entry<String, ConsumerGroupEntity> key : cache.entrySet()) {
                if(key.getValue()!=null){
                    boolean mirroFlag = key.getValue().getOriginName().equals(consumerGroupEntity.getName())
                            && key.getValue().getId() != consumerGroupEntity.getId();
                    if(mirroFlag){
                        return new ConsumerGroupDeleteResponse("1","存在镜像组,不能删除原始组");
                    }
                }

            }
        }


        return doDelete(consumerGroupEntity,checkOnline);
    }

    @Override
    public ConsumerGroupTopicEntity getTopic(String consumerGroupName, String topicName) {
        Map<String, ConsumerGroupEntity> cache = getCache();
        if(!cache.containsKey(consumerGroupName)){
            return null;
        }
        if (!consumerGroupTopicService.getCache().containsKey(cache.get(consumerGroupName).getId())) {
            return null;
        }
        return consumerGroupTopicService.getCache().get(cache.get(consumerGroupName).getId()).get(topicName);
    }

    @Override
    public ConsumerGroupCreateResponse createConsumerGroup(ConsumerGroupCreateRequest consumerGroupCreateRequest) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumerGroupEditResponse editConsumerGroup(ConsumerGroupEntity consumerGroupEntity) {

        CacheUpdateHelper.updateCache();
        ConsumerGroupEntity oldConsumerGroupEntity = get(consumerGroupEntity.getId());
        consumerGroupEntity.setTopicNames(oldConsumerGroupEntity.getTopicNames());
        consumerGroupEntity.setRbVersion(oldConsumerGroupEntity.getRbVersion());
        consumerGroupEntity.setMetaVersion(oldConsumerGroupEntity.getMetaVersion());
        consumerGroupEntity.setVersion(oldConsumerGroupEntity.getVersion());
        consumerGroupEntity.setInsertBy(oldConsumerGroupEntity.getInsertBy());
        consumerGroupEntity.setIsActive(oldConsumerGroupEntity.getIsActive());
        consumerGroupEntity.setOriginName(oldConsumerGroupEntity.getOriginName());

        String userId = userInfoHolder.getUserId();
        consumerGroupEntity.setUpdateBy(userId);

        //如果是广播模式，原始消费者和镜像消费者都要更新
        if(consumerGroupEntity.getMode()==2){
            //用原始名称为key去更新数据
            updateOriginName(consumerGroupEntity)
        }else {
            update(consumerGroupEntity);
        }

        // 如果修改了appid字段，则需要修改consumerGroup对应的失败topic的AppId
        if (!StringUtils.equals(oldConsumerGroupEntity.getAppId(), consumerGroupEntity.getAppId())) {
            topicService.updateFailTopic(consumerGroupEntity);
        }

        //如果修改了消费模式，queueoffset 的消费模式也要修改
        if(consumerGroupEntity.getMode()!=oldConsumerGroupEntity.getMode()){
            Map<String, List<QueueOffsetEntity>> map = queueOffsetService.getConsumerGroupQueueOffsetMap();
            for (QueueOffsetEntity queueOffset : map.get(consumerGroupEntity.getName())) {
                queueOffset.setConsumerGroupMode(consumerGroupEntity.getMode());
                queueOffsetService.update(queueOffset);
            }
        }

        consumerGroupTopicService.updateEmailByGroupName(consumerGroupEntity.getName(),
                consumerGroupEntity.getAlarmEmails());
        AuditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, oldConsumerGroupEntity.getId(),
                "更新ConsumerGroup:" + consumerGroupEntity.getName() + ".更新信息："
                        + AuditUtil.diff(oldConsumerGroupEntity, consumerGroupEntity));
        notifyMeta(oldConsumerGroupEntity.getId());

        ConsumerGroupEditResponse consumerGroupEditResponse = new ConsumerGroupEditResponse();
        consumerGroupEditResponse.setMsg("success");
        return consumerGroupEditResponse;
    }


    /**
     * 删除消费者组,广播模式专用
     * 1. 检查是否权限
     * 2. 检查是否正在消费
     * 3. 删除偏移,队列,consumergroup
     * 4. 通知重平衡
     * @param consumerGroupEntity entity need delete
     * @param checkOnline 是否online 过来
     * @return result
     */
    private ConsumerGroupDeleteResponse doDelete(ConsumerGroupEntity consumerGroupEntity, boolean checkOnline) {
        if (checkOnline) {
            if (roleService.getRole(userInfoHolder.getUserId(), consumerGroupEntity.getOwnerIds()) >= UserRoleEnum.USER
                    .getRoleCode()) {
                throw new RuntimeException();
            }
        }
        //正在消费的不能cancel
        List<Long> consumerGroupIds = new ArrayList<>();
        consumerGroupIds.add(consumerGroupEntity.getId());
        if (checkOnline && consumerService.getConsumerGroupByConsumerGroupIds(consumerGroupIds).size() > 0) {
            return new ConsumerGroupDeleteResponse("1", "有消费者正在消费，不能删除消费者组。");
        }
        //广播模式下,删除广播的失败队列
        List<String> failTopicNames = consumerGroupTopicService.getFailTopicNames(consumerGroupEntity.getId());
        topicService.deleteFailTopic(failTopicNames,consumerGroupEntity.getId());
        queueOffsetService.deleteByConsumerGroupId(consumerGroupEntity.getId());
        consumerGroupTopicService.deleteByConsumerGroupId(consumerGroupEntity.getId());
        delete(consumerGroupEntity.getId());
        notifyMeta(consumerGroupEntity.getId());
        return new ConsumerGroupDeleteResponse();
    }

    protected void updateMetaVersion(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids))
            return;
        consumerGroupRepository.updateMetaVersion(ids);
    }

    private void updateRbVersion(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return;
        consumerGroupRepository.updateRbVersion(ids);

    }

    @Override
    public String getCacheJson() {
        return JsonUtil.toJsonNull(getCache());
    }

    @Override
    public void start() {
        if(startFlag.compareAndSet(false,true)){
            updateCache();
            ScheduledExecutorService service = Executors.newScheduledThreadPool(1, SoaThreadFactory.create("ConsumerGroupService_"));
            service.submit(()->{
                while (isRunning){
                    updateCache();
                    Util.sleep(soaConfig.getMqConsumerGroupCacheInterval());
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
