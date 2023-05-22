package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.CacheUpdateHelper;
import com.baracklee.mq.biz.ui.dto.request.QueueOffsetAccumulationRequest;
import com.baracklee.mq.biz.ui.dto.request.QueueOffsetGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.QueueOffsetGetListResponse;
import com.baracklee.mq.biz.ui.dto.response.QueueOffsetIntelligentDetectionResponse;
import com.baracklee.mq.biz.ui.dto.response.QueueOffsetUpdateResponse;
import com.baracklee.mq.biz.ui.dto.response.QueueOffsetUpdateStopFlagResponse;
import com.baracklee.mq.biz.ui.enums.NodeTypeEnum;
import com.baracklee.mq.biz.ui.exceptions.AuthFailException;
import com.baracklee.mq.biz.ui.vo.QueueOffsetVo;
import com.baracklee.ui.spi.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
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

        for (QueueOffsetEntity queueOffset : queueOffsetList) {
            QueueOffsetVo queueOffsetVo = new QueueOffsetVo(queueOffset);
            QueueEntity queueEntity = queueMap.get(queueOffset.getQueueId());

            // maxId为最大id+1
            long maxId = 0;
            long currentMaxId = 0;
            if (queueMaxIdMap.get(queueEntity.getId()) != null) {
                maxId = queueMaxIdMap.get(queueEntity.getId());
            }
            // 如果当前偏移大于缓存中的最大Id
            if (queueOffsetVo.getOffset() > (maxId - 1)) {
                if (flag) {
                    try {
                        message01Service.setDbId(queueEntity.getDbNodeId());
                        currentMaxId = queueService.getMaxId(queueEntity.getId(), queueEntity.getTbName());
                    } catch (Exception e) {
                        flag = false;
                    }
                }

            } else {
                currentMaxId = maxId;
            }
            String key = queueOffset.getConsumerGroupName() + "_" + queueOffset.getTopicName();
            if (!consumerGroupTopicMap.containsKey(key)) {
                consumerGroupTopicService.updateCache();
                consumerGroupTopicMap = consumerGroupTopicService.getGroupTopic();

            }
            int maxLag = consumerGroupTopicMap.get(key).getMaxLag();
            queueOffsetVo.setMaxLag(maxLag);
            queueOffsetVo.setPendingMessageNum(currentMaxId - 1 - queueOffset.getOffset());
            queueOffsetVo.setMinusMaxLag(queueOffsetVo.getPendingMessageNum() - maxLag);
            if (consumerGroupMap.get(queueOffsetVo.getConsumerGroupName()) != null) {
                queueOffsetVo.setConsumerGroupOwners(
                        consumerGroupMap.get(queueOffsetVo.getConsumerGroupName()).getOwnerNames());
            }

            queueOffsetVoList.add(queueOffsetVo);
            // 统计consumer不为空的consumerGroup
            if (StringUtils.isNotEmpty(queueOffsetVo.getConsumerName())) {
                if (usingConsumerGroupMap.containsKey(queueOffsetVo.getConsumerGroupName())) {
                    usingConsumerGroupMap.get(queueOffsetVo.getConsumerGroupName()).add(queueOffsetVo);
                } else {
                    List<QueueOffsetVo> list = new ArrayList<>();
                    list.add(queueOffsetVo);
                    usingConsumerGroupMap.put(queueOffsetVo.getConsumerGroupName(), list);
                }
            }
        }
        queueOffsetSort(queueOffsetVoList);
        queueOffsetVos.set(queueOffsetVoList);
        usingConsumerGroups.set(usingConsumerGroupMap);
        return true;
    }

    private void queueOffsetSort(List<QueueOffsetVo> queueOffsetVoList) {
        queueOffsetVoList.sort((q1,q2)->{
            long l = q1.getMinusMaxLag() - q2.getMinusMaxLag();
            if (l==0) return 0;
            else if(l>0) return -1;
            else return 1;
        });
    }

    public void deleteByQueueId(Long queueId, Long topicId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(QueueOffsetEntity.FdQueueId, queueId);
        List<QueueOffsetEntity> queueOffsetEntityList = queueOffsetService.getList(conditionMap);
        List<Long> ids = queueOffsetEntityList.stream().map(QueueOffsetEntity::getId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ids)) {
            queueOffsetService.delete(ids);
            auditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                    "移除queue[" + queueId + "]时，删除queueoffset, " + JsonUtil.toJson(queueOffsetEntityList));
        }
    }

    public QueueOffsetGetListResponse findBy(QueueOffsetGetListRequest queueOffsetGetListRequest) {
        Map<String, Object> parameterMap = new HashMap<>();
        //通过originConsumerGroupName查询
        if(StringUtils.isNotEmpty(queueOffsetGetListRequest.getConsumerGroupName())){
            parameterMap.put("consumerGroupName", queueOffsetGetListRequest.getConsumerGroupName());
        }
        parameterMap.put("topicName", queueOffsetGetListRequest.getTopicName());
        parameterMap.put("consumerName", queueOffsetGetListRequest.getConsumerName());
        parameterMap.put("topicType", queueOffsetGetListRequest.getTopicType());
        parameterMap.put("subEnv",queueOffsetGetListRequest.getSubEnv());
        if(StringUtils.isNotEmpty(queueOffsetGetListRequest.getMode())){
            parameterMap.put("consumerGroupMode",Integer.parseInt(queueOffsetGetListRequest.getMode()));
        }
        if (StringUtils.isNotBlank(queueOffsetGetListRequest.getId())) {
            parameterMap.put("id", Long.valueOf(queueOffsetGetListRequest.getId()));
        }
        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();

        Map<Long, Long> queueMaxIdMap = queueService.getMax();

        long count = queueOffsetService.countBy(parameterMap);
        List<QueueOffsetEntity> queueOffsetList = queueOffsetService.getListBy(parameterMap,
                Long.valueOf(queueOffsetGetListRequest.getPage()), Long.valueOf(queueOffsetGetListRequest.getLimit()));
        List<QueueOffsetVo> queueOffsetVoList = new LinkedList<>();
        boolean flag = true;
        for (QueueOffsetEntity queueOffsetEntity : queueOffsetList) {

            QueueEntity queueEntity = queueService.get(queueOffsetEntity.getQueueId());
            DbNodeEntity dbNodeEntity = dbNodeService.get(queueEntity.getDbNodeId());

            QueueOffsetVo queueOffsetVo = new QueueOffsetVo(queueOffsetEntity);

            ConsumerGroupEntity consumerGroupEntity = consumerGroupMap.get(queueOffsetEntity.getConsumerGroupName());
            String owners = "";
            if (consumerGroupEntity != null) {
                owners = consumerGroupEntity.getOwnerIds();
            }
            // 筛选读写类型
            if (StringUtils.isNotEmpty(queueOffsetGetListRequest.getReadOnly())) {
                if (Integer.parseInt(queueOffsetGetListRequest.getReadOnly()) != queueEntity.getReadOnly()) {
                    continue;
                }
            }
            int role = roleService.getRole(userInfoHolder.getUserId(), owners);
            queueOffsetVo.setRole(role);
            queueOffsetVo.setNodeType(queueEntity.getNodeType());
            queueOffsetVo.setMinId(queueEntity.getMinId());

            // 如果数据库节点为只读，则队列的读写状态不受数据库节点的影响
            if (dbNodeEntity.getReadOnly() == 1) {
                queueOffsetVo.setReadOnly(queueEntity.getReadOnly());
            } else {
                // 如果数据库节点不是只读时，则该节点下所有队列的读写状态失效
                queueOffsetVo.setReadOnly(dbNodeEntity.getReadOnly());
            }
            // maxId为最大id+1
            long maxId = queueMaxIdMap.get(queueEntity.getId());
            long currentMaxId = 0;
            // 如果当前偏移大于缓存中的最大Id
            if (queueOffsetVo.getOffset() > (maxId - 1)&&maxId!=queueOffsetVo.getOffset()) {
                if (flag) {
                    try {
                        message01Service.setDbId(queueEntity.getDbNodeId());
                        currentMaxId = queueService.getMaxId(queueEntity.getId(), queueEntity.getTbName());
                    } catch (Exception e) {
                        flag = false;
                    }
                }

            } else {
                currentMaxId = maxId;
            }
            queueOffsetVo.setMessageNum(currentMaxId - 1 - queueEntity.getMinId());
            if(queueOffsetVo.getMessageNum()<0){
                queueOffsetVo.setMessageNum(0);
            }
            queueOffsetVo.setPendingMessageNum(currentMaxId - 1 - queueOffsetEntity.getOffset());
            if(queueOffsetVo.getPendingMessageNum()<0){
                queueOffsetVo.setPendingMessageNum(0);
            }
            if(currentMaxId>queueOffsetVo.getMinId()){
                queueOffsetVo.setMaxId(currentMaxId - 1);
            }else{
                queueOffsetVo.setMaxId(currentMaxId);
            }
            queueOffsetVoList.add(queueOffsetVo);

        }

        return new QueueOffsetGetListResponse(count, queueOffsetVoList);
    }

    public BaseUiResponse<List<QueueOffsetVo>> findAccumulation(QueueOffsetAccumulationRequest queueOffsetAccumulationRequest) {
        lastAccessTime = System.currentTimeMillis();
        int page = Integer.parseInt(queueOffsetAccumulationRequest.getPage());
        int pageSize = Integer.parseInt(queueOffsetAccumulationRequest.getLimit());
        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();

        List<QueueOffsetVo> queueOffsetVoList = new LinkedList<>();
        List<QueueOffsetVo> cache=queueOffsetVos.get();
        for (QueueOffsetVo queueOffsetVo : cache) {

            if (StringUtils.isNotEmpty(queueOffsetAccumulationRequest.getConsumerGroupName())) {
                //通过原始消费者组名字去匹配查询
                if (!queueOffsetVo.getOriginConsumerGroupName().equals(queueOffsetAccumulationRequest.getConsumerGroupName())) {
                    continue;
                }
            }

            if (StringUtils.isNotEmpty(queueOffsetAccumulationRequest.getTopicName())) {
                if (!queueOffsetVo.getTopicName().equals(queueOffsetAccumulationRequest.getTopicName())) {
                    continue;
                }
            }

            if (StringUtils.isNotEmpty(queueOffsetAccumulationRequest.getId())) {
                if (queueOffsetVo.getId() != Integer.parseInt(queueOffsetAccumulationRequest.getId())) {
                    continue;
                }
            }

            if (StringUtils.isNotEmpty(queueOffsetAccumulationRequest.getOwnerNames())) {
                if (!consumerGroupMap.get(queueOffsetVo.getConsumerGroupName()).getOwnerNames()
                        .contains(queueOffsetAccumulationRequest.getOwnerNames())) {
                    continue;
                }
            }

            // 获取在线堆积时，如果该消费者组不存在consumer，则排除
            if ("1".equals(queueOffsetAccumulationRequest.getOnlineType())) {
                if (!usingConsumerGroups.get().containsKey(queueOffsetVo.getConsumerGroupName())) {
                    continue;
                }
            }

            // 获取离线堆积时，如果该消费者组存在一个consumer，则排除
            if ("2".equals(queueOffsetAccumulationRequest.getOnlineType())) {
                if (usingConsumerGroups.get().containsKey(queueOffsetVo.getConsumerGroupName())) {
                    continue;
                }
            }

            //获取负责人异常的消费者组, 只要有一个负责人还存在，则跳过
            if("3".equals(queueOffsetAccumulationRequest.getOnlineType())){
                List<String> ownerIds=Arrays.asList(consumerGroupMap.get(queueOffsetVo.getConsumerGroupName()).getOwnerIds().split(","));
                if(isOwnerAvailable(ownerIds)){
                    continue;
                }

            }

            queueOffsetVoList.add(queueOffsetVo);

        }

        int t = queueOffsetVoList.size();
        if ((page * pageSize) > queueOffsetVoList.size()) {
            queueOffsetVoList = queueOffsetVoList.subList((page - 1) * pageSize, queueOffsetVoList.size());
        } else {
            queueOffsetVoList = queueOffsetVoList.subList((page - 1) * pageSize, page * pageSize);
        }
        return new BaseUiResponse<>(new Long(t), queueOffsetVoList);
    }

    public BaseUiResponse<Object> accumulationAlert(String ownerName){
        QueueOffsetAccumulationRequest queueOffsetAccumulationRequest=new QueueOffsetAccumulationRequest();
        queueOffsetAccumulationRequest.setOwnerNames(ownerName);
        queueOffsetAccumulationRequest.setPage("1");
        queueOffsetAccumulationRequest.setLimit("10");
        List<QueueOffsetVo> queueOffsetVoList = findAccumulation(queueOffsetAccumulationRequest).getData();
        BaseUiResponse<Object> baseUiResponse = new BaseUiResponse<>();
        Map<String,String> queueOffsetVoMap=new HashMap<>();
        Map<String,TopicEntity> topicMap=topicService.getCache();
        StringBuilder re=new StringBuilder();
        if(queueOffsetVoList.size()>0){
            re.append("您名下的consumerGroup:");
            for (QueueOffsetVo queueOffsetVo:queueOffsetVoList) {
                TopicEntity topic=topicMap.get(queueOffsetVo.getTopicName());
                if(topic!=null&&queueOffsetVo.getPendingMessageNum()>topic.getMaxLag()&&!queueOffsetVoMap.containsKey(queueOffsetVo.getConsumerGroupName())){
                    re.append(queueOffsetVo.getConsumerGroupName()+",");
                    queueOffsetVoMap.put(queueOffsetVo.getConsumerGroupName(),queueOffsetVo.getConsumerGroupName());
                }
            }
            re.append("消费慢，产生了堆积，请尽快处理！");
        }
        if(!queueOffsetVoMap.isEmpty()){
            baseUiResponse.setCode("0");
            baseUiResponse.setMsg(re.toString());
        }
        else{
            baseUiResponse.setCode("1");
            baseUiResponse.setMsg("");
        }

        return baseUiResponse;
    }


    public List<QueueOffsetEntity> doFindAllBy(Long topicId) {
        Map<String, Object> conditionMap = new HashMap<>();
        TopicEntity topicEntity = topicService.get(topicId);
        if (topicEntity==null){
            return null;
        }
        conditionMap.put(QueueOffsetEntity.FdQueueId,topicId);
        List<QueueOffsetEntity> queueOffsetList = queueOffsetService.getList(conditionMap);
        List<TopicEntity> failTopicEntities = getFailTopic(topicEntity.getName());
        for (TopicEntity topicEntity1 : failTopicEntities) {
            conditionMap.put(QueueOffsetEntity.FdTopicId, topicEntity1.getId());
            queueOffsetList.addAll(queueOffsetService.getList(conditionMap));
        }
        return queueOffsetList;

    }

    public List<TopicEntity> getFailTopic(String topicName) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(TopicEntity.FdOriginName, topicName);
        conditionMap.put(TopicEntity.FdTopicType, NodeTypeEnum.FAIL_NODE_TYPE.getTypeCode());
        return topicService.getList(conditionMap);
    }

    public QueueOffsetVo findById(long queueOffsetId){
        Map<Long, Long> queueMaxIdMap = queueService.getMax();
        QueueOffsetEntity queueOffsetEntity = queueOffsetService.get(queueOffsetId);
        QueueOffsetVo queueOffsetVo = new QueueOffsetVo(queueOffsetEntity);
        QueueEntity queueEntity = queueService.get(queueOffsetEntity.getQueueId());
        long maxId = queueMaxIdMap.get(queueOffsetEntity.getQueueId());
        queueOffsetVo.setMinId(queueEntity.getMinId());
        if(maxId>queueOffsetVo.getMinId()){
            queueOffsetVo.setMaxId(maxId - 1);
        }else{
            queueOffsetVo.setMaxId(maxId);
        }
        return queueOffsetVo;
    }

    public QueueOffsetUpdateResponse updateQueueOffset(long id, long offset) {
        CacheUpdateHelper.updateCache();
        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        Map<Long, QueueEntity> queueMap = queueService.getAllQueueMap();
        QueueOffsetEntity originQueueOffsetEntity = queueOffsetService.get(id);
        if (roleService.getRole(userInfoHolder.getUserId(),
                consumerGroupMap.get(originQueueOffsetEntity.getConsumerGroupName()).getOwnerIds()) >= UserRoleEnum.USER
                .getRoleCode()) {
            throw new AuthFailException("没有操作权限，请进行权限检查。");
        }
        // 偏移不能大于当前的最大ID
        QueueEntity queueEntity = queueMap.get(originQueueOffsetEntity.getQueueId());
        message01Service.setDbId(queueEntity.getDbNodeId());
        // maxId为最大id+1
        long maxId = queueService.getMaxId(originQueueOffsetEntity.getQueueId(), queueEntity.getTbName());
        if (offset > (maxId - 1)) {
            return new QueueOffsetUpdateResponse("1", "偏移值不能大于当前的最大Id");
        }
        if (offset < queueEntity.getMinId()) {
            return new QueueOffsetUpdateResponse("1", "偏移值不能小于当前的最小Id");
        }
        String updateBy = userInfoHolder.getUserId();

        Map<String, Object> parameterMap = new LinkedHashMap<String, Object>();
        parameterMap.put(QueueOffsetEntity.FdId, id);
        parameterMap.put(QueueOffsetEntity.FdUpdateBy, updateBy);
        parameterMap.put(QueueOffsetEntity.FdOffset, offset);
        queueOffsetService.updateQueueOffset(parameterMap);
        consumerGroupService.notifyOffset(originQueueOffsetEntity.getConsumerGroupId());
        consumerGroupService.notifyMeta(originQueueOffsetEntity.getConsumerGroupId());
        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, originQueueOffsetEntity.getConsumerGroupId(),
                "queueOffset的偏移从" + originQueueOffsetEntity.getOffset() + "变为" + offset + "。原始queueOffset信息为："
                        + JsonUtil.toJson(originQueueOffsetEntity));
        return new QueueOffsetUpdateResponse();
    }

    public QueueOffsetUpdateStopFlagResponse updateStopFlag(long id, int stopFlag) {
        CacheUpdateHelper.updateCache();
        QueueOffsetEntity originQueueOffsetEntity = queueOffsetService.get(id);
        String updateBy = userInfoHolder.getUserId();
        queueOffsetService.updateStopFlag(id, stopFlag, updateBy);
        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, originQueueOffsetEntity.getConsumerGroupId(),
                "消费标志从" + originQueueOffsetEntity.getStopFlag() + "变为" + stopFlag);
        consumerGroupService.notifyMeta(originQueueOffsetEntity.getConsumerGroupId());
        return new QueueOffsetUpdateStopFlagResponse();
    }


    public String getByConsumerGroupTopic(long consumerGroupTopicId) {
        ConsumerGroupTopicEntity consumerGroupTopicEntity = consumerGroupTopicService.get(consumerGroupTopicId);
        List<QueueOffsetEntity> queueOffsetEntityList = queueOffsetService.getByConsumerGroupTopic(
                consumerGroupTopicEntity.getConsumerGroupId(), consumerGroupTopicEntity.getTopicId());
        for (QueueOffsetEntity queueOffset : queueOffsetEntityList) {
            if (StringUtils.isNotEmpty(queueOffset.getConsumerName())) {
                return queueOffset.getConsumerName();
            }
        }
        return null;
    }

    public QueueOffsetIntelligentDetectionResponse intelligentDetection(long queueOffsetId){
        QueueOffsetEntity queueOffsetEntity = queueOffsetService.get(queueOffsetId);;
        Map<Long, QueueEntity> queueMap=queueService.getAllQueueMap();
        QueueEntity queueEntity=queueMap.get(queueOffsetEntity.getQueueId());
        long minId=queueEntity.getMinId();
        Map<String, ConsumerGroupTopicEntity> groupTopicMap=consumerGroupTopicService.getGroupTopic();
        String consumerGroupName=queueOffsetEntity.getConsumerGroupName();
        String topicName=queueOffsetEntity.getTopicName();
        ConsumerGroupTopicEntity consumerGroupTopic=groupTopicMap.get(consumerGroupName+"_"+topicName);
        message01Service.setDbId(queueEntity.getDbNodeId());
        Long tableMinId=message01Service.getTableMinId(queueEntity.getTbName());
        String result="正常，无需修复";
        if(tableMinId!=null){
            if((queueOffsetEntity.getOffset()+consumerGroupTopic.getPullBatchSize())<(tableMinId-1)){
                if((tableMinId-1)!=minId){
                    //schema表引起的最小id不准确问题
                    queueEntity.setMinId(tableMinId-1);
                    queueService.update(queueEntity);
                }
                queueOffsetEntity.setOffset(tableMinId-1);
                queueOffsetService.update(queueOffsetEntity);
                result="修复成功";

                consumerGroupService.notifyMeta(queueOffsetEntity.getConsumerGroupId());
            }
        }

        if(queueOffsetEntity.getOffset()<minId){
            queueOffsetEntity.setOffset(minId);
            queueOffsetService.update(queueOffsetEntity);
            result="修复成功";
            consumerGroupService.notifyMeta(queueOffsetEntity.getConsumerGroupId());
        }

        return new QueueOffsetIntelligentDetectionResponse("0",result);

    }


    public QueueOffsetGetListResponse findAllBy(Long topicId){
        List<QueueOffsetEntity> queueOffsetList = doFindAllBy(topicId);
        if (CollectionUtils.isEmpty(queueOffsetList)) {
            return new QueueOffsetGetListResponse(0L, null);
        }

        List<QueueOffsetVo> queueOffsetVos = queueOffsetList.stream().map(queueOffsetEntity -> {
            QueueOffsetVo queueOffsetVo = new QueueOffsetVo(queueOffsetEntity);
            ConsumerGroupEntity consumerGroupEntity = consumerGroupService.get(queueOffsetEntity.getConsumerGroupId());
            if (consumerGroupEntity != null) {
                queueOffsetVo.setConsumerGroupOwners(consumerGroupEntity.getOwnerNames());
                queueOffsetVo.setConsumerGroupOwnerIds(consumerGroupEntity.getOwnerIds());
            }
            return queueOffsetVo;
        }).collect(Collectors.toList());
        return new QueueOffsetGetListResponse((long) queueOffsetVos.size(), queueOffsetVos);
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
