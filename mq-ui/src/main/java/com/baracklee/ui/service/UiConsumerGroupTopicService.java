package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dal.meta.ConsumerGroupTopicRepository;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AuditUtil;
import com.baracklee.mq.biz.service.common.CacheUpdateHelper;
import com.baracklee.mq.biz.ui.dto.request.ConsumerGroupTopicGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicEditResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicGetListResponse;
import com.baracklee.mq.biz.ui.exceptions.AuthFailException;
import com.baracklee.mq.biz.ui.vo.ConsumerGroupTopicVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Barack Lee
 */
@Service
public class UiConsumerGroupTopicService {

    private ConsumerGroupTopicRepository consumerGroupTopicRepository;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private UiConsumerGroupService uiConsumerGroupService;

    private ConsumerGroupService consumerGroupService;

    private AuditLogService auditLogService;

    private RoleService roleService;

    private SoaConfig soaConfig;

    private UserInfoHolder userInfoHolder;

    @Autowired
    public UiConsumerGroupTopicService(ConsumerGroupTopicRepository consumerGroupTopicRepository,
                                       ConsumerGroupTopicService consumerGroupTopicService,
                                       UiConsumerGroupService uiConsumerGroupService,
                                       ConsumerGroupService consumerGroupService,
                                       AuditLogService auditLogService,
                                       RoleService roleService,
                                       SoaConfig soaConfig,
                                       UserInfoHolder userInfoHolder) {
        this.consumerGroupTopicRepository = consumerGroupTopicRepository;
        this.consumerGroupTopicService = consumerGroupTopicService;
        this.uiConsumerGroupService = uiConsumerGroupService;
        this.consumerGroupService = consumerGroupService;
        this.auditLogService = auditLogService;
        this.roleService = roleService;
        this.soaConfig = soaConfig;
        this.userInfoHolder = userInfoHolder;
    }


    public ConsumerGroupTopicGetListResponse findBy(ConsumerGroupTopicGetListRequest consumerGroupTopicGetListRequest) {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(ConsumerGroupTopicEntity.FdConsumerGroupName,
                consumerGroupTopicGetListRequest.getConsumerGroupName());
        parameterMap.put(ConsumerGroupTopicEntity.FdTopicName, consumerGroupTopicGetListRequest.getTopicName());
        parameterMap.put(ConsumerGroupTopicEntity.FdConsumerGroupId,
                consumerGroupTopicGetListRequest.getConsumerGroupId());
        long count = consumerGroupTopicService.count(parameterMap);

        List<ConsumerGroupTopicEntity> consumerGroupTopicList = consumerGroupTopicService.getList(parameterMap);

        // 缓存数据
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();

        List<ConsumerGroupTopicVo> consumerGroupTopicVoList = consumerGroupTopicList.stream().map(
                value -> {
                    ConsumerGroupTopicVo consumerGroupTopicVo = new ConsumerGroupTopicVo(value);
                    consumerGroupTopicVo.setRole(roleService.getRole(userInfoHolder.getUserId(),
                            consumerGroupMap.get(value.getConsumerGroupName()).getOwnerIds()));
                    return consumerGroupTopicVo;
                }
        ).collect(Collectors.toList());
        return new ConsumerGroupTopicGetListResponse(count,consumerGroupTopicVoList);

    }

    public List<ConsumerGroupTopicEntity> findByTopicId(Long topicId) {
        Map<String, Object> conditionMap = new HashMap<>();
        conditionMap.put(ConsumerGroupTopicEntity.FdTopicId, topicId);
        return consumerGroupTopicService.getList(conditionMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConsumerGroupTopicEditResponse editConsumerGroupTopic(ConsumerGroupTopicEntity consumerGroupTopicEntity){
        CacheUpdateHelper.updateCache();

        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();

        String consumerGroupName = consumerGroupTopicEntity.getConsumerGroupName();

        ConsumerGroupEntity consumerGroupEntity = consumerGroupMap.get(consumerGroupName);

        //如果是广播模式原始消费组,对原始组下面所有镜像进行编辑更新
        if(consumerGroupEntity.getMode()==2&&consumerGroupEntity.getOriginName().equals(consumerGroupEntity.getName())){
            doUpdateByOrigin(consumerGroupMap,consumerGroupTopicEntity,consumerGroupEntity);
        }
        doUpdate(consumerGroupTopicEntity,consumerGroupMap);

        return new ConsumerGroupTopicEditResponse();

    }

    private void doUpdateByOrigin(Map<String, ConsumerGroupEntity> consumerGroupMap,
                                  ConsumerGroupTopicEntity originConsumerGroupTopicEntity,
                                  ConsumerGroupEntity originConsumerGroupEntity) {
        Map<String, List<ConsumerGroupTopicEntity>> topicSubscribeMap = consumerGroupTopicService
                .getTopicSubscribeMap();

        //被编辑的consumerGroupTopic下的topic, 所对应的consumerGroupTopic 列表
        List<ConsumerGroupTopicEntity> consumerGroupTopicEntities
                = topicSubscribeMap.get(originConsumerGroupTopicEntity.getOriginTopicName());
        for (ConsumerGroupTopicEntity groupTopic : consumerGroupTopicEntities) {
            //如果是镜像消费者
            if(consumerGroupMap.get(groupTopic.getConsumerGroupName()).getOriginName().equals(originConsumerGroupEntity.getName())){
                if (groupTopic.getId()!=originConsumerGroupTopicEntity.getId()){
                    groupTopic.setRetryCount(originConsumerGroupTopicEntity.getRetryCount());
                    groupTopic.setMaxLag(originConsumerGroupTopicEntity.getMaxLag());
                    groupTopic.setTag(originConsumerGroupTopicEntity.getTag());
                    groupTopic.setDelayProcessTime(originConsumerGroupTopicEntity.getDelayProcessTime());
                    groupTopic.setMaxPullTime(originConsumerGroupTopicEntity.getMaxPullTime());
                    groupTopic.setThreadSize(originConsumerGroupTopicEntity.getThreadSize());
                    groupTopic.setPullBatchSize(originConsumerGroupTopicEntity.getPullBatchSize());
                    groupTopic.setConsumerBatchSize(originConsumerGroupTopicEntity.getConsumerBatchSize());
                    groupTopic.setAlarmEmails(originConsumerGroupTopicEntity.getAlarmEmails());
                    groupTopic.setTimeOut(originConsumerGroupTopicEntity.getTimeOut());
                    doUpdate(groupTopic, consumerGroupMap);
                }
            }
        }

    }

    private void doUpdate(ConsumerGroupTopicEntity consumerGroupTopicEntity, Map<String, ConsumerGroupEntity> consumerGroupMap) {
        ConsumerGroupTopicEntity originConsumerGroupTopic = consumerGroupTopicService
                .get(consumerGroupTopicEntity.getId());

        if (roleService.getRole(userInfoHolder.getUserId(),
                consumerGroupMap.get(originConsumerGroupTopic.getConsumerGroupName())
                        .getOwnerIds()) >= UserRoleEnum.USER.getRoleCode()) {
            throw new AuthFailException("没有操作权限，请进行权限检查。");
        }


        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("consumerGroupId", originConsumerGroupTopic.getConsumerGroupId());
        parameterMap.put("originTopicName", originConsumerGroupTopic.getOriginTopicName());
        parameterMap.put("topicType", originConsumerGroupTopic.getTopicType());

        //更新的的正常队列,correspond 就是异常的,更新的是异常的,correspond 就是正常的
        ConsumerGroupTopicEntity correspondConsumerGroupTopicEntity = consumerGroupTopicService
                .getCorrespondConsumerGroupTopic(parameterMap);
        // 为了使得正常topic和失败topic的重试次数一致
        correspondConsumerGroupTopicEntity.setRetryCount(consumerGroupTopicEntity.getRetryCount());
        correspondConsumerGroupTopicEntity.setTag(consumerGroupTopicEntity.getTag());
        correspondConsumerGroupTopicEntity.setAlarmEmails(consumerGroupTopicEntity.getAlarmEmails());
        correspondConsumerGroupTopicEntity.setTimeOut(consumerGroupTopicEntity.getTimeOut());
        String userId = userInfoHolder.getUserId();
        consumerGroupTopicEntity.setUpdateBy(userId);

        consumerGroupTopicService.update(consumerGroupTopicEntity);
        consumerGroupTopicService.update(correspondConsumerGroupTopicEntity);

        auditLogService.recordAudit(ConsumerGroupEntity.TABLE_NAME, consumerGroupTopicEntity.getConsumerGroupId(),
                "编辑" + consumerGroupTopicEntity.getConsumerGroupName() + "下的consumerGroupTopic"
                        + AuditUtil.diff(originConsumerGroupTopic, consumerGroupTopicEntity));
        consumerGroupService.notifyMeta(originConsumerGroupTopic.getConsumerGroupId());
    }


    public ConsumerGroupTopicEntity findById(long consumerGroupTopicId){
        return consumerGroupTopicRepository.getById(consumerGroupTopicId);
    }

    public ConsumerGroupTopicCreateRequest initConsumerGroupTopic(long consumerGroupId) {
        ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest = new ConsumerGroupTopicCreateRequest();
        ConsumerGroupEntity consumerGroupEntity = uiConsumerGroupService.findById(consumerGroupId);

        // String userId = userInfoHolder.getUserId();
        String email = userInfoHolder.getUser().getEmail();
        consumerGroupTopicCreateRequest.setAlarmEmails(email);
        consumerGroupTopicCreateRequest.setConsumerGroupId(consumerGroupId);
        consumerGroupTopicCreateRequest.setConsumerGroupName(consumerGroupEntity.getName());
        consumerGroupTopicCreateRequest.setThreadSize(soaConfig.getConsumerGroupTopicThreadSize());
        consumerGroupTopicCreateRequest.setRetryCount(soaConfig.getConsumerGroupTopicRetryCount());
        consumerGroupTopicCreateRequest.setMaxLag(soaConfig.getConsumerGroupTopicLag());
        consumerGroupTopicCreateRequest.setDelayProcessTime(soaConfig.getDelayProcessTime());
        consumerGroupTopicCreateRequest.setPullBatchSize(soaConfig.getPullBatchSize());
        consumerGroupTopicCreateRequest.setConsumerBatchSize(soaConfig.getConsumerBatchSize());
        consumerGroupTopicCreateRequest.setDelayPullTime(soaConfig.getMaxDelayPullTime());
        consumerGroupTopicCreateRequest.setTimeOut(0);
        return consumerGroupTopicCreateRequest;
    }



    }
