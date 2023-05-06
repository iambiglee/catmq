package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.Constants;
import com.baracklee.mq.biz.dto.request.TopicCreateRequest;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AuditUtil;
import com.baracklee.mq.biz.service.common.CacheUpdateHelper;
import com.baracklee.mq.biz.ui.dto.request.TopicGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.TopicCreateResponse;
import com.baracklee.mq.biz.ui.dto.response.TopicDeleteResponse;
import com.baracklee.mq.biz.ui.dto.response.TopicExpandResponse;
import com.baracklee.mq.biz.ui.dto.response.TopicGetListResponse;
import com.baracklee.mq.biz.ui.enums.NodeTypeEnum;
import com.baracklee.mq.biz.ui.exceptions.AuthFailException;
import com.baracklee.mq.biz.ui.exceptions.CheckFailException;
import com.baracklee.mq.biz.ui.vo.QueueVo;
import com.baracklee.mq.biz.ui.vo.TopicVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.invoke.MethodHandles;
import java.util.*;
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
public class UiTopicService implements TimerService {
    Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TopicService topicService;

    private UiQueueService uiQueueService;

    private SoaConfig soaConfig;

    private AuditLogService uiAuditLogService;

    private DbNodeService dbNodeService;

    private UiQueueOffsetService uiQueueOffsetService;

    private QueueService queueService;

    private ConsumerGroupService consumerGroupService;

    private UiConsumerGroupTopicService uiConsumerGroupTopicService;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private RoleService roleService;

    private Message01Service message01Service;

    private UserInfoHolder userInfoHolder;

@Autowired
    public UiTopicService(TopicService topicService,
                          UiQueueService uiQueueService,
                          SoaConfig soaConfig,
                          AuditLogService uiAuditLogService,
                          DbNodeService dbNodeService,
                          UiQueueOffsetService uiQueueOffsetService,
                          QueueService queueService,
                          ConsumerGroupService consumerGroupService,
                          UiConsumerGroupTopicService uiConsumerGroupTopicService,
                          ConsumerGroupTopicService consumerGroupTopicService,
                          RoleService roleService,
                          Message01Service message01Service,
                          UserInfoHolder userInfoHolder) {
        this.topicService = topicService;
        this.uiQueueService = uiQueueService;
        this.soaConfig = soaConfig;
        this.uiAuditLogService = uiAuditLogService;
        this.dbNodeService = dbNodeService;
        this.uiQueueOffsetService = uiQueueOffsetService;
        this.queueService = queueService;
        this.consumerGroupService = consumerGroupService;
        this.uiConsumerGroupTopicService = uiConsumerGroupTopicService;
        this.consumerGroupTopicService = consumerGroupTopicService;
        this.roleService = roleService;
        this.message01Service = message01Service;
        this.userInfoHolder = userInfoHolder;
    }
    private ThreadPoolExecutor executor = null;
    private volatile boolean isRunning = true;
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private AtomicReference<List<TopicVo>> topicVoListRf = new AtomicReference<>(new ArrayList<>());
    private volatile long lastUpdateTime = 0;
    private final String shouldShrink = "应该缩容";
    private final String shouldExpand = "应该扩容";

    private volatile long lastAccessTime = System.currentTimeMillis() * 2;

    @Override
    public void start() {
        if (startFlag.compareAndSet(false, true)) {
            lastUpdateTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
            executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    SoaThreadFactory.create("UiTopicService", true), new ThreadPoolExecutor.DiscardOldestPolicy());
            executor.execute(() -> {
                while (isRunning) {
                    try {
                        if (System.currentTimeMillis() - lastAccessTime < soaConfig.getMqReportInterval()
                                || System.currentTimeMillis() - lastAccessTime > 1000 * 60 * 60 * 60) {
                            if (System.currentTimeMillis() - lastUpdateTime > soaConfig.getMqReportInterval()) {
                                initCache();
                                if (uiQueueService.getQueueListCount().size() > 0) {
                                    lastUpdateTime = System.currentTimeMillis();
                                    lastAccessTime = System.currentTimeMillis() - soaConfig.getMqReportInterval() * 2;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        log.error("UiQueueServiceImpl_initCache_error", e);
                    }
                    if (uiQueueService.getQueueListCount().size() == 0) {
                        Util.sleep(10 * 1000);
                    } else {
                        Util.sleep(2 * 1000);
                    }
                }
            });
        }
    }

    private void initCache() {
        Map<String, TopicEntity> topicMap = topicService.getCache();
        Map<String, List<QueueEntity>> topicQueueMap = queueService.getAllLocatedTopicQueue();
        Map<String, List<QueueVo>> queueVoMap = uiQueueService.getQueueListCount();
        List<TopicVo> topicVoList = new ArrayList<>();
        for (String topicName : topicMap.keySet()) {
            try {
                int queueCount = 0;
                TopicVo topicVo = new TopicVo(topicMap.get(topicName));
                long msgCount = 0;
                if (queueVoMap != null) {
                    List<QueueVo> queueList = queueVoMap.get(topicName);
                    if (queueList != null) {
                        for (QueueVo queueVo : queueList) {
                            msgCount += queueVo.getMsgCount();
                        }
                    }

                }
                topicVo.setMsgCount(msgCount);
                if (topicQueueMap.containsKey(topicName)) {
                    queueCount = topicQueueMap.get(topicName).size();
                }

                topicVo.setQueueCount(queueCount);
                if (topicVo.getSaveDayNum() > 0) {
                    topicVo.setAvgCount(topicVo.getMsgCount() / topicVo.getSaveDayNum());
                }
                if (topicVo.getQueueCount() > 0) {
                    topicVo.setAvgCountOfQueue(topicVo.getAvgCount() / topicVo.getQueueCount());
                }
                if (topicVo.getAvgCountOfQueue() < 500000 && topicVo.getQueueCount() > 1) {
                    topicVo.setIsReasonable(shouldShrink);
                } else if (topicVo.getAvgCountOfQueue() > 1000000) {
                    topicVo.setIsReasonable(shouldExpand);
                }
                // 根据topic每天的平均消息量，计算该topic合理的队列数量（向上取整算法）
                long reasonableQueueCount = ((topicVo.getAvgCount() + 999999)
                        - (topicVo.getAvgCount() + 999999) % 1000000) / 1000000;
                topicVo.setManageQueueCount(reasonableQueueCount - topicVo.getQueueCount());
                topicVoList.add(topicVo);
            } catch (Exception e) {
                throw new RuntimeException(topicName, e);
            }
        }
        topicSort(topicVoList);
        topicVoListRf.set(topicVoList);
    }

    private void topicSort(List<TopicVo> topicVoList) {
        // 按照消息总量
        topicVoList.sort((q1, q2) -> {
            long i = q1.getMsgCount() - q2.getMsgCount();
            if (i == 0) {
                return 0;
            } else if (i > 0) {
                return -1;
            } else {
                return 1;
            }
        });
    }


    public TopicGetListResponse queryByPage(TopicGetListRequest topicGetListRequest) {
        Map<String, List<QueueEntity>> topicQueueMap = queueService.getAllLocatedTopicQueue();
        Map<String, Object> conditionMap = new HashMap<>();
        if (StringUtils.isNotBlank(topicGetListRequest.getName())) {
            conditionMap.put("name", topicGetListRequest.getName());
        }
        if (StringUtils.isNotBlank(topicGetListRequest.getId())) {
            conditionMap.put("id", Long.valueOf(topicGetListRequest.getId()));
        }
        if (StringUtils.isNotBlank(topicGetListRequest.getOwnerName())) {
            conditionMap.put(TopicEntity.FdOwnerNames, topicGetListRequest.getOwnerName());
        }
        if (StringUtils.isNotBlank(topicGetListRequest.getTopicType())) {
            conditionMap.put(TopicEntity.FdTopicType, topicGetListRequest.getTopicType());
        }
        long count = topicService.countWithUserName(conditionMap);
        if (count == 0) {
            return new TopicGetListResponse(count, null);
        }
        List<TopicEntity> topicEntityList = topicService.getListWithUserName(conditionMap,
                Long.parseLong(topicGetListRequest.getPage()), Long.parseLong(topicGetListRequest.getLimit()));
        String currentUserId = userInfoHolder.getUserId();
        List<TopicVo> topicVoList = topicEntityList.stream().map(topicEntity -> {
            TopicVo topicVo = new TopicVo(topicEntity);
            topicVo.setRole(roleService.getRole(currentUserId, topicEntity.getOwnerIds()));
            int queueCount = 0;
            if (topicQueueMap.containsKey(topicEntity.getName())) {
                queueCount = topicQueueMap.get(topicEntity.getName()).size();
            } else {
                queueCount = topicEntity.getExpectDayCount() / Constants.NUMS_OF_MESSAGE_PER_QUEUE_ONEDAY;
            }

            topicVo.setQueueCount(queueCount);
            return topicVo;
        }).collect(Collectors.toList());

        return new TopicGetListResponse(count, topicVoList);
    }
    private boolean hasAuth(String userId, TopicEntity topicEntity) {
        return Arrays.asList(topicEntity.getOwnerIds().split(",")).contains(userId) || roleService.isAdmin(userId);
    }

    private boolean isAdmin(String userId) {
        return roleService.isAdmin(userId);
    }


    public TopicCreateResponse createOrUpdateTopic(TopicCreateRequest topicCreateRequest) {
        CacheUpdateHelper.updateCache();
        String name = topicCreateRequest.getName();
        if (name.length() > 4 && "fail".equals(name.substring(name.length() - 4).toLowerCase())) {
            throw new CheckFailException("topic名称:" + name + "不能以fail结尾");
        }
        TopicEntity topicEntity = new TopicEntity();
        topicCreateRequest.setName(StringUtils.trim(topicCreateRequest.getName()));
        topicEntity.setName(topicCreateRequest.getName());
        topicEntity.setOwnerIds(topicCreateRequest.getOwnerIds());
        topicEntity.setOwnerNames(topicCreateRequest.getOwnerNames());
        topicEntity.setExpectDayCount(topicCreateRequest.getExpectDayCount());
        topicEntity.setEmails(StringUtils.trim(topicCreateRequest.getEmails()));
        topicEntity.setBusinessType(topicCreateRequest.getBusinessType());
        topicEntity.setMaxLag(topicCreateRequest.getMaxLag());
        topicEntity.setRemark(topicCreateRequest.getRemark());
        topicEntity.setDptName(topicCreateRequest.getDptName());
        topicEntity.setOriginName(topicCreateRequest.getName());
        topicEntity.setNormalFlag(topicCreateRequest.getNormalFlag());
        topicEntity.setSaveDayNum(topicCreateRequest.getSaveDayNum());
        topicEntity.setTels(topicCreateRequest.getTels());
        topicEntity.setIsActive(1);
        topicEntity.setTopicType(topicCreateRequest.getTopicType());
        topicEntity.setConsumerFlag(topicCreateRequest.getConsumerFlag());
        topicEntity.setConsumerGroupNames(topicCreateRequest.getConsumerGroupList());
        topicEntity.setAppId(topicCreateRequest.getAppId());
        String userId = userInfoHolder.getUserId();
        if (StringUtils.isNotEmpty(topicCreateRequest.getId())) {
            topicEntity.setId(Long.valueOf(topicCreateRequest.getId()));
            topicEntity.setUpdateBy(userId);
            updateTopic(topicEntity);
        } else {
            topicEntity.setInsertBy(userId);
            createSuccessTopic(topicEntity);
        }
        // 创建或者更新topic时，同步到mq2
        // synService32.synTopic32(topicCreateRequest, topicEntity);
        return new TopicCreateResponse();
    }

    private void updateTopic(TopicEntity topicEntity) {
        String currentUserId = userInfoHolder.getUserId();
        TopicEntity oldTopicEntity = baseCheckRequest(topicEntity.getId(), currentUserId);
        // 鉴于安全原因，token 不能传给前端，只能在服务端传递
        topicEntity.setToken(oldTopicEntity.getToken());
        topicService.update(topicEntity);
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(),
                "更新topic，" + AuditUtil.diff(oldTopicEntity, topicEntity));
    }

    private TopicEntity baseCheckRequest(Long topicId, String currentUserId) {
        TopicEntity topicEntity = topicService.get(topicId);
        if (topicEntity == null) {
            throw new CheckFailException("topic已经被删除，请刷新重试。 ");
        }
        if (!hasAuth(currentUserId, topicEntity)) {
            throw new AuthFailException("没有操作权限，请进行权限检查。");
        }
        return topicEntity;
    }
    private void createSuccessTopic(TopicEntity topicEntity) {
        TopicEntity entity= topicService.getTopicByName(topicEntity.getName());
        if(entity!=null){
            throw new CheckFailException("topic:"+topicEntity.getName()+"重复，检查是否有重名topic已经存在。");
        }
        try {
            topicService.insert(topicEntity);
        } catch (DuplicateKeyException e) {
            throw new CheckFailException("topic:" + topicEntity.getName() + "重复，检查是否有重名topic已经存在。");
        }
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(),
                "新建topic，" + JsonUtil.toJson(topicEntity));
        // 计算分配的队列数
        int expectDayCount = topicEntity.getExpectDayCount() * 10000;
        int successQueueNum = expectDayCount / Constants.MSG_NUMS_OF_ONE_QUEUE;
        // 分配队列
        topicService.distributeQueueWithLock(topicEntity, successQueueNum,
                NodeTypeEnum.SUCCESS_NODE_TYPE.getTypeCode());
    }


    public TopicDeleteResponse deleteTopic(Long topicId) {
        CacheUpdateHelper.updateCache();
        String currentUserId = userInfoHolder.getUserId();
        Map<String, List<QueueVo>> queueVoMap = uiQueueService.getQueueListCount();
        TopicEntity topicEntity = baseCheckRequest(topicId, currentUserId);

        if (uiConsumerGroupTopicService.findByTopicId(topicId).size() > 0) {
            throw new CheckFailException("目前有消费者订阅，不能删除，请通知取消订阅后再删除");
        }

        // 如果topic下存在消息量大于阈值的queue，则不允许删除
        if (queueVoMap != null && soaConfig.isPro()) {
            List<QueueVo> queueList = queueVoMap.get(topicEntity.getName());
            if (queueList != null) {
                for (QueueVo queueVo : queueList) {
                    if (queueVo.getMsgCount() > soaConfig.getTopicDeleteLimitCount() * 10000L) {
                        throw new CheckFailException("topic:" + topicEntity.getName() + "存在消息量大于"
                                + soaConfig.getTopicDeleteLimitCount() + "万的queue,不能直接删除");
                    }
                }
            }
        }

        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(),
                "删除topic之前" + JsonUtil.toJson(topicEntity));
        doDeleteTopic(topicEntity);
        // 删除topic后，同步到mq2
        // synService32.synTopicDelete(topicEntity);
        return new TopicDeleteResponse();
    }

    private void doDeleteTopic(TopicEntity topicEntity) {
        Long topicId = topicEntity.getId();
        List<QueueEntity> queueEntities = queueService.getQueuesByTopicId(topicId);
        if (soaConfig.isPro() && !isAdmin(userInfoHolder.getUserId())) {
            queueEntities.forEach(queueEntity -> uiQueueService.remove(queueEntity.getId()));
        } else {
            queueEntities.forEach(queueEntity -> uiQueueService.forceRemove(queueEntity.getId()));
        }
        topicService.delete(topicId);
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(),
                "删除topic，" + JsonUtil.toJson(topicEntity));
    }

    public List<TopicEntity> getFailTopic(String topicName) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(TopicEntity.FdOriginName, topicName);
        conditionMap.put(TopicEntity.FdTopicType, NodeTypeEnum.FAIL_NODE_TYPE.getTypeCode());
        return topicService.getList(conditionMap);
    }

    public TopicExpandResponse expandTopic(Long topicId) {
        String currentUserId = userInfoHolder.getUserId();
        TopicEntity topicEntity = baseCheckRequest(topicId, currentUserId);
        if (roleService.getRole(userInfoHolder.getUserId()) > 0) {
            List<QueueEntity> queueEntities = queueService.getQueuesByTopicId(topicId);
            checkQueueMessageCount(queueEntities);
            checkQueueMax(queueEntities);
        }
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId, "开始扩容, 扩容队列 1 条");
        List<QueueEntity> normalQueueList = queueService.getTopUndistributed(1, topicEntity.getTopicType(), topicId);
        if (CollectionUtils.isEmpty(normalQueueList)) {
            uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId, "数据节点不够分配");
            throw new CheckFailException("数据节点不够分配，请联系管理员");
        }
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                String.format("对备选队列进行分配： %s", normalQueueList.get(0).getId()));
        topicService.distributeQueue(topicEntity, normalQueueList.get(0));
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId, "扩容结束");
        uiQueueOffsetService.createQueueOffsetForExpand(normalQueueList.get(0), topicId, topicEntity);
        uiAuditLogService.recordAudit(TopicEntity.TABLE_NAME, topicId,
                "分配queue，" + JsonUtil.toJson(normalQueueList.get(0).getId()));
        consumerGroupService.notifyRb(uiConsumerGroupTopicService.findByTopicId(topicId).stream()
                .map(ConsumerGroupTopicEntity::getConsumerGroupId).collect(Collectors.toList()));

        return new TopicExpandResponse();
    }


    private void checkQueueMessageCount(List<QueueEntity> queueEntities) {
        if (!soaConfig.getMaxTableMessageSwitch()) {
            return;
        }
        if (CollectionUtils.isEmpty(queueEntities)) {
            return;
        }
        Long allMessageCount = 0L;

        for (QueueEntity queueEntity : queueEntities) {
            allMessageCount += getQueueMessage(queueEntity);
        }

        if (allMessageCount / queueEntities.size() > soaConfig.getMaxTableMessage()) {
            throw new CheckFailException("每队列消息量未达到最大值，不允许扩容，可联系管理员强制扩容");
        }
    }

    private void checkQueueMax(List<QueueEntity> queueEntities) {
        int maxQueue = soaConfig.getMaxQueuePerTopic();
        if (CollectionUtils.isEmpty(queueEntities)) {
            return;
        }
        if (queueEntities.size() >= maxQueue) {
            throw new CheckFailException("topic内队列数量达到上限，不允许扩容，可联系管理员强制扩容");
        }
    }

    private Long getQueueMessage(QueueEntity queueEntity) {
        message01Service.setDbId(queueEntity.getDbNodeId());
        Long maxId = message01Service.getMaxId(queueEntity.getTbName());
        Long minId = queueEntity.getMinId();
        return maxId - minId - 1;
    }



    @Override
    public void stop() {

    }

    @Override
    public String info() {
        return null;
    }
}
