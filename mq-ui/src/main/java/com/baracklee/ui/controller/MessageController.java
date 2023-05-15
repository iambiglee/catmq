package com.baracklee.ui.controller;

import com.alibaba.fastjson2.JSONArray;
import com.baracklee.mq.biz.MqConst;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dto.client.PublishMessageResponse;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.RoleService;
import com.baracklee.mq.biz.service.TopicService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.ui.dto.request.MessageConditionRequest;
import com.baracklee.mq.biz.ui.dto.request.MessageGetByTopicRequest;
import com.baracklee.mq.biz.ui.dto.request.MessageGetListRequest;
import com.baracklee.mq.biz.ui.dto.request.MessageToolRequest;
import com.baracklee.mq.biz.ui.dto.response.MessageConditionResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageGetByTopicResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageGetListResponse;
import com.baracklee.mq.client.MqClient;
import com.baracklee.ui.service.UiMessageService;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Barack Lee
 */

@RestController
@RequestMapping("/message")
public class MessageController {

    private UiMessageService uiMessageService;

    private Environment environment;

    private AuditLogService auditLogService;

    private TopicService topicService;

    private UserInfoHolder userInfoHolder;

    private RoleService roleService;

    private SoaConfig soaConfig;


    public MessageController(UiMessageService uiMessageService,
                             Environment environment,
                             AuditLogService auditLogService,
                             TopicService topicService,
                             UserInfoHolder userInfoHolder,
                             RoleService roleService,
                             SoaConfig soaConfig) {
        this.uiMessageService = uiMessageService;
        this.environment = environment;
        this.auditLogService = auditLogService;
        this.topicService = topicService;
        this.userInfoHolder = userInfoHolder;
        this.roleService = roleService;
        this.soaConfig = soaConfig;
    }

    @RequestMapping("/list/data")
    public MessageGetListResponse getMessageByPage(MessageGetListRequest messageGetListRequest) {
        return uiMessageService.getMessageByPage(messageGetListRequest);
    }

    @RequestMapping("/list/condition")
    public MessageConditionResponse searchCondition(MessageConditionRequest messageConditionRequest) {
        return uiMessageService.getMessageRange(messageConditionRequest);
    }

    @RequestMapping("/list/topicQueueIds")
    @ResponseBody
    public List<QueueEntity> getQueueEntity(@RequestParam(name = "topicName") String topicName) {
        List<QueueEntity> queueEntities = uiMessageService.getQueueByTopicName(topicName);
        return queueEntities;
    }

    @RequestMapping("/queue/slave")
    @ResponseBody
    public int checkQueueSlave(@RequestParam(name = "queueId") long queueId) {
        int queueSlave = uiMessageService.checkQueueSlave(queueId);
        return queueSlave;
    }

    @RequestMapping("/retry/failMessage")
    @ResponseBody
    public PublishMessageResponse sendMessage(long queueId, long messageId) {
        List<Long>ids=new ArrayList<>();
        ids.add(messageId);
        return uiMessageService.sendAllFailMessage(queueId, ids);
    }

    @RequestMapping("/tool/sendMessage")
    @ResponseBody
    public PublishMessageResponse sendMessageByTool(@RequestBody MessageToolRequest messageToolRequest)
            throws Exception {
        PublishMessageResponse publishMessageResponse = new PublishMessageResponse();
        Map<String,TopicEntity> topicMap=topicService.getCache();
        TopicEntity topicEntity=topicMap.get(messageToolRequest.getTopicName());
        int userRole=2;
        if(topicEntity!=null){
            userRole=roleService.getRole(userInfoHolder.getUserId(),topicEntity.getOwnerIds());
        }
        boolean isPro = soaConfig.isPro();
        if(isPro){
            //生产环境中，如果用户不是系统管理员，也不是topic负责人，则不能往该topic发送消息
            if(userRole!=0&&userRole!=1){
                publishMessageResponse.setCode("1");
                publishMessageResponse.setMsg("你不是topic负责人，不能发送消息");
                return publishMessageResponse;
            }
        }

        boolean result = false;
        if (messageToolRequest != null) {
            if (messageToolRequest.getMqSubEnv()!=null&&!MqConst.DEFAULT_SUBENV.equalsIgnoreCase(messageToolRequest.getMqSubEnv())) {

                if(messageToolRequest.getMessage().getHead()==null){
                    Map<String, String> headMap=new HashMap<>();
                    headMap.put(MqConst.MQ_SUB_ENV_KEY, messageToolRequest.getMqSubEnv());
                    messageToolRequest.getMessage().setHead(headMap);
                }else{
                    messageToolRequest.getMessage().getHead().put(MqConst.MQ_SUB_ENV_KEY, messageToolRequest.getMqSubEnv());
                }

            }

            if (!StringUtils.isEmpty(messageToolRequest.getTopicName())
                    && !StringUtils.isEmpty(messageToolRequest.getMessage().getBody())) {
                result = MqClient.publish(messageToolRequest.getTopicName(), environment.getProperty("test-token", ""),
                        messageToolRequest.getMessage());
            }
        }


        //发送成功，添加审计日志
        if (result) {
            auditLogService.recordAudit(TopicEntity.TABLE_NAME, topicEntity.getId(), "用户" + userInfoHolder.getUserId() + "通过管理界面往topic" +
                    topicEntity.getName() + "中发送了消息：" + JsonUtil.toJson(messageToolRequest.getMessage()));
            publishMessageResponse.setCode("0");
            publishMessageResponse.setMsg("发送成功！");
        }else{
            publishMessageResponse.setCode("1");
            publishMessageResponse.setMsg("发送失败！");
        }
        return publishMessageResponse;
    }


    @RequestMapping("/retryAll/failMessage")
    @ResponseBody
    public PublishMessageResponse retryAllFailMessage(@RequestParam("messageIds") String messageIds,long queueId) {
        List<String> consumerIdList = JSONArray.parseArray(messageIds, String.class);
        List<Long>ids=new ArrayList<>();
        for (String id:consumerIdList) {
            ids.add(Long.parseLong(id));
        }
        return uiMessageService.sendAllFailMessage(queueId, ids);
    }

    @RequestMapping("/getByTopic")
    public MessageGetByTopicResponse getMessageByTopic(@RequestBody MessageGetByTopicRequest messageGetByTopicRequest){
        return uiMessageService.getMessageByTopic(messageGetByTopicRequest);
    }


}
