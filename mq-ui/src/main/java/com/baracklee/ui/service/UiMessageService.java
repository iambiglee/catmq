package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.client.PublishMessageResponse;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.Message01Entity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.Message01Service;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.TopicService;
import com.baracklee.mq.biz.ui.dto.request.MessageConditionRequest;
import com.baracklee.mq.biz.ui.dto.request.MessageGetByTopicRequest;
import com.baracklee.mq.biz.ui.dto.request.MessageGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.MessageConditionResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageGetByTopicResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageGetListResponse;
import com.baracklee.mq.biz.ui.vo.MessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UiMessageService {
    private Message01Service message01Service;
    private QueueService queueService;

    private DbNodeService dbNodeService;
    private TopicService topicService;
    private SoaConfig soaConfig;

    @Autowired
    public UiMessageService(Message01Service message01Service,
                            QueueService queueService,
                            DbNodeService dbNodeService,
                            TopicService topicService,
                            SoaConfig soaConfig) {
        this.message01Service = message01Service;
        this.queueService = queueService;
        this.dbNodeService = dbNodeService;
        this.topicService = topicService;
        this.soaConfig = soaConfig;
    }

    public int checkQueueSlave(long queueId){
        QueueEntity queue=queueService.get(queueId);
        if(queue!=null){
            Map<Long, DbNodeEntity> dbNodeMap=dbNodeService.getCache();
            if(dbNodeMap.containsKey(queue.getDbNodeId())){
                return dbNodeService.hasSlave(dbNodeMap.get(queue.getDbNodeId()))?1:0;
            }
        }
        return 0;
    }

    public List<QueueEntity> getQueueByTopicName(String topicName) {
        List<QueueEntity> result = null;
        Map<String, Object> parameter = new HashMap<>(16);
        parameter.put(QueueEntity.FdTopicName, topicName);
        result = queueService.getList(parameter);
        return result;
    }

    public MessageGetListResponse getMessageByPage(MessageGetListRequest messageGetListRequest) {
        Map<String, Object> parameterMap = new HashMap<>();
        if (StringUtils.isEmpty(messageGetListRequest.getQueueId())) {
            return new MessageGetListResponse(new Long(0), null);
        }
        long pageSize=Long.valueOf(messageGetListRequest.getLimit());
        long start = (Long.valueOf(messageGetListRequest.getPage()) - 1) * pageSize;
        long maxId, minId, count = 0;
        List<MessageVo> messageVos = new ArrayList<>();
        QueueEntity queueEntity = queueService.get(Long.valueOf(messageGetListRequest.getQueueId()));
        maxId = messageGetListRequest.getMaxId();
        minId = messageGetListRequest.getMinId();
        count = maxId - minId - 1;
        if(count==0){
            return new MessageGetListResponse(count, null);
        }

        message01Service.setDbId(queueEntity.getDbNodeId());
        TopicEntity topicEntity = topicService.get(queueEntity.getTopicId());
        List<Message01Entity> message01EntityList=new ArrayList<>();

        if (topicEntity.getTopicType()==1){
            //指定ID去查询
            if (messageGetListRequest.getId()!=0L){
                Message01Entity message = message01Service.getMessageById(queueEntity.getTbName(), messageGetListRequest.getId());
                message01EntityList.add(message);
            }
            //分页查询
            else if (StringUtils.isEmpty(messageGetListRequest.getBizId())&&StringUtils.isEmpty(messageGetListRequest.getTraceId())
                    &&StringUtils.isEmpty(messageGetListRequest.getHeader())&&StringUtils.isEmpty(messageGetListRequest.getBody())){
                long end1 = maxId-start-1;
                long start1=end1-pageSize;
                if (start1<minId){
                    start1=minId;
                }
                message01EntityList = message01Service.getListDy(queueEntity.getTopicName(),queueEntity.getTbName(), start1, end1);
            }
            //指定唯一条件查询，不分页
            else {
                parameterMap.put("tbName",message01Service.getDbName()+"."+queueEntity.getTbName());
                parameterMap.put("bizId", messageGetListRequest.getBizId());
                parameterMap.put("traceId", messageGetListRequest.getTraceId());
                parameterMap.put("head", messageGetListRequest.getHeader());
                parameterMap.put("body", messageGetListRequest.getBody());
                parameterMap.put("start1",start);
                parameterMap.put("offset1",pageSize);
                parameterMap.put("maxId",maxId);
                parameterMap.put("minId",minId);

                message01EntityList = message01Service.getListByPage(parameterMap);

                message01Service.setDbId(queueEntity.getDbNodeId());
                count=message01Service.countByPage(parameterMap);

            }
        }
        else if (topicEntity.getTopicType()==2){
            //如果是失败topic，则根据正常分页逻辑查询
            parameterMap.put("tbName",message01Service.getDbName()+"."+queueEntity.getTbName());
            parameterMap.put("bizId", messageGetListRequest.getBizId());
            parameterMap.put("traceId", messageGetListRequest.getTraceId());
            parameterMap.put("head", messageGetListRequest.getHeader());
            parameterMap.put("body", messageGetListRequest.getBody());
            parameterMap.put("start1",start);
            parameterMap.put("offset1",pageSize);
            parameterMap.put("startTime",messageGetListRequest.getStartTime());
            parameterMap.put("endTime",messageGetListRequest.getEndTime());
            if(!StringUtils.isEmpty(messageGetListRequest.getRetryStatus())){
                parameterMap.put("retryStatus",Integer.parseInt(messageGetListRequest.getRetryStatus()));
                parameterMap.put("failMsgRetryCountSuc",Message01Service.failMsgRetryCountSuc);
            }
            message01EntityList = message01Service.getListByPage(parameterMap);

            message01Service.setDbId(queueEntity.getDbNodeId());
            count=message01Service.countByPage(parameterMap);
        }

        //进行数据处理
        for (Message01Entity message01Entity : message01EntityList) {
            if (message01Entity!=null){
                MessageVo messageVo = new MessageVo(message01Entity);
                if (topicEntity != null) {
                    messageVo.setType(topicEntity.getTopicType());
                }
                //失败topic,设置消息重试转态
                if (messageVo.getType()==2){
                    if (messageVo.getRetryCount()>Message01Service.failMsgRetryCountSuc){
                        messageVo.setFailMsgRetryStatus("重试成功");
                        messageVo.setRetryCount(messageVo.getRetryCount()-Message01Service.failMsgRetryCountSuc);
                    }else {
                        messageVo.setFailMsgRetryStatus("重试失败");
                    }
                }

                if("null".equals(messageVo.getTag())){
                    messageVo.setTag(null);
                }
                messageVos.add(messageVo);
            }
        }
        return new MessageGetListResponse(count,messageVos);
    }


    public PublishMessageResponse sendAllFailMessage(long queueId, List<Long> messageIds){
        QueueEntity queueEntity = queueService.get(Long.valueOf(queueId));
        message01Service.setDbId(queueEntity.getDbNodeId());
        List<Message01Entity>failMessages=message01Service.getMessageByIds(queueEntity.getTbName(),messageIds);

        failMessages.forEach(t1->t1.setRetryCount(1));
        message01Service.setDbId(queueEntity.getDbNodeId());
        message01Service.insertBatchDy(queueEntity.getTopicName(),queueEntity.getTbName(),failMessages);
        //重复发送之后，删除原来的旧消息
        message01Service.setDbId(queueEntity.getDbNodeId());
        message01Service.deleteByIds(queueEntity.getTbName(),messageIds);
        PublishMessageResponse submitMessageResponse=new PublishMessageResponse();
        submitMessageResponse.setSuc(true);
        return submitMessageResponse;
    }

    /**
     * 查找最小ID，如果有时间，查找时间范围最小的id
     */
    private long getMinId(Message01Service message01Service, QueueEntity queueEntity,
                          MessageConditionRequest messageConditionRequest){
        long minId = queueEntity.getMinId();
        if(StringUtils.isEmpty(messageConditionRequest.getStartTime())){
            List<Message01Entity> list = message01Service.getListByTime(queueEntity.getTbName(), messageConditionRequest.getStartTime());
            if (!CollectionUtils.isEmpty(list)) {
                Message01Entity message01Entity = list.get(0);
                minId = message01Entity.getId();
            }
        }
        return minId;
    }

    /**
     * 查找最大id，初始值为1，若设置截止时间，则取到截止时间的最后一条消息的id，若没有则取队列最大id
     */
    private long getMaxId(Message01Service message01Service, QueueEntity queueEntity,
                          MessageConditionRequest messageConditionRequest){
        long maxId = 1;
        message01Service.setDbId(queueEntity.getDbNodeId());
        if (StringUtils.isEmpty(messageConditionRequest.getEndTime())) {
            maxId = message01Service.getMaxId(queueEntity.getTbName());
        } else {
            List<Message01Entity> list = message01Service.getListByTime(queueEntity.getTbName(),
                    messageConditionRequest.getEndTime());
            if (!CollectionUtils.isEmpty(list)) {
                Message01Entity message01Entity = list.get(list.size() - 1);
                maxId = message01Entity.getId();
                maxId++;
            }
        }
        return maxId;
    }

    public MessageConditionResponse getMessageRange(MessageConditionRequest messageConditionRequest) {
        QueueEntity queueEntity = queueService.get(Long.valueOf(messageConditionRequest.getQueueId()));
        Map<String, Object> data = new HashMap<>();
        long maxId = getMaxId(message01Service, queueEntity, messageConditionRequest);
        long minId = getMinId(message01Service, queueEntity, messageConditionRequest);
        data.put("minId", minId);
        data.put("maxId", maxId);
        return new MessageConditionResponse(data);
    }

    public MessageGetByTopicResponse getMessageByTopic(MessageGetByTopicRequest messageGetByTopicRequest){
        Map<String,TopicEntity> topicMap=topicService.getCache();
        if(StringUtils.isEmpty(messageGetByTopicRequest.getBizId())||StringUtils.isEmpty(messageGetByTopicRequest.getTopicName())){
            return new MessageGetByTopicResponse("1","业务id和topic不能同时为空");
        }
        if (!topicMap.containsKey(messageGetByTopicRequest.getTopicName())){
            return new MessageGetByTopicResponse("1","topic不存在");
        }

        List<QueueEntity> queueList=queueService.getQueuesByTopicId(topicMap.get(messageGetByTopicRequest.getTopicName()).getId());
        List<Message01Entity> messageList = new ArrayList<>();
        Map<String, Object> parameterMap = new HashMap<>();
        for (QueueEntity queueEntity : queueList) {
            message01Service.setDbId(queueEntity.getDbNodeId());
            parameterMap.put("tbName",message01Service.getDbName()+"."+queueEntity.getTbName());
            parameterMap.put("bizId", messageGetByTopicRequest.getBizId());
            parameterMap.put("start1",0);
            parameterMap.put("offset1",1000);
            message01Service.setDbId(queueEntity.getDbNodeId());
            messageList.addAll(message01Service.getListByPage(parameterMap));
        }
        return new MessageGetByTopicResponse(messageList);
    }

}
