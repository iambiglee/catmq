package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.MqConst;
import com.baracklee.mq.biz.MqEnv;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.*;
import com.baracklee.mq.biz.dal.meta.ConsumerRepository;
import com.baracklee.mq.biz.dto.LogDto;
import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.NotifyFailVo;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.base.PartitionInfo;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.entity.*;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.client.MqClient;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ConsumerServiceImpl extends AbstractBaseService<ConsumerEntity> implements ConsumerService {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Resource
    private ConsumerRepository consumerRepository;
    @Resource
    private LogService logService;
    @Resource
    private ConsumerGroupService consumerGroupService;
    @Autowired
    private ConsumerGroupTopicService consumerGroupTopicService;
    @Resource
    SoaConfig soaConfig;
    @Resource
    private ConsumerGroupConsumerService consumerGroupConsumerService;

    @Resource
    private TopicService topicService;

    @Resource
    QueueOffsetService queueOffsetService;

    @Resource
    QueueService queueService;

    @Resource
    DbNodeService dbNodeService;

    @Resource
    Message01Service message01Service;

    @Resource
    private EmailUtil emailUtil;

    @Resource
    AuditLogService auditLogService;

    @PostConstruct
    void init(){
        super.setBaseRepository(consumerRepository);
    }

    //记录topic和dbNode失败时间
    protected Map<String, Long> dbFailMap = new ConcurrentHashMap<>();

    // 记录消息推送通知的时间
    private AtomicReference<Map<Long, Long>> speedLimitMapRef = new AtomicReference<Map<Long, Long>>(
            new ConcurrentHashMap<>(1000));
    //记录消息推送失败的消息
    private AtomicReference<Map<String, NotifyFailVo>> notifyFailMapRef = new AtomicReference<Map<String, NotifyFailVo>>(
            new ConcurrentHashMap<>(1000));


    private AtomicReference<Map<String, AtomicInteger>> counter = new AtomicReference<Map<String, AtomicInteger>>(
            new ConcurrentHashMap<>(1000));
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumerRegisterResponse register(ConsumerRegisterRequest request) {
        ConsumerRegisterResponse response = new ConsumerRegisterResponse();
        response.setSuc(true);
        checkVaild(request,response);
        if(!response.isSuc()){
            return response;
        }
        addRegisterLog(request);
        ConsumerEntity consumerEntity=doRegisterConsumer(request);
        response.setId(consumerEntity.getId());
        if (soaConfig.getSdkVersion().compareTo(request.getSdkVersion()) > 0) {
            response.setMsg(
                    "当前mq3客户端的版本已经落后了，最新版本为:" + soaConfig.getSdkVersion() + ",当前版本为:" + request.getSdkVersion());
        }
        return response;
    }

    @Override
    public ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request) {
        ConsumerGroupRegisterResponse response = new ConsumerGroupRegisterResponse();
        if (null==request){
            response.setSuc(false);
            response.setMsg("request cannot be null");
            return response;
        }
        ConsumerEntity consumerEntity=get(request.getConsumerId());
        if(null==consumerEntity){
            response.setSuc(false);
            response.setMsg("ConsumerId_" + request.getConsumerId() + "不存在！");
            return response;
        }
        response.setBroadcastConsumerGroupName(new HashMap<>());
        response.setConsumerGroupNameNew(new HashMap<>());
        //检查广播模式，广播模式数据可以被每一个消费者消费
        checkBroadcastAndSubEnv(request,response);
        doRegisterConsumerGroup(request,response,consumerEntity);
        if (!response.isSuc()) {
            addRegisterConsumerGroupLog(request, response);
        }
        return response;
    }
    protected void addRegisterConsumerGroupLog(ConsumerGroupRegisterRequest request,
                                               ConsumerGroupRegisterResponse response) {
        String json = JsonUtil.toJsonNull(request);
        if (request != null && request.getConsumerGroupNames() != null) {
            List<AuditLogEntity> auditLogs = new ArrayList<>(request.getConsumerGroupNames().size());
            Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
            request.getConsumerGroupNames().keySet().forEach(t1 -> {
                ConsumerGroupEntity temp = consumerGroupMap.get(t1);
                if (temp != null) {
                    AuditLogEntity auditLog = new AuditLogEntity();
                    auditLog.setContent("注册失败！入参是：" + json + ",原因是:" + response.getMsg());
                    auditLog.setTbName(ConsumerGroupEntity.TABLE_NAME);
                    auditLog.setRefId(temp.getId());
                    auditLogs.add(auditLog);
                }
            });
            auditLogService.insertBatch(auditLogs);
        } else {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setContent("注册失败！入参是：" + json + ",原因是:" + response.getMsg());
            auditLog.setTbName(ConsumerGroupEntity.TABLE_NAME);
            auditLog.setRefId(0);
            auditLogService.insert(auditLog);
        }
    }
    @Override
    public List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerGroupIds(List<Long> consumerGroupIds) {
        if(CollectionUtils.isEmpty(consumerGroupIds)){
            return new ArrayList<>();
        }
        return consumerGroupConsumerService.getByConsumerGroupIds(consumerGroupIds);
    }

    @Override
    public List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerIds(List<Long> consumerIds) {
        if(CollectionUtils.isEmpty(consumerIds)) return new ArrayList<>();
        return consumerGroupConsumerService.getByConsumerIds(consumerIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumerDeRegisterResponse deRegister(ConsumerDeRegisterRequest request) {
        ConsumerDeRegisterResponse response = new ConsumerDeRegisterResponse();
        response.setSuc(true);
        if(request==null||request.getId()==0){
            response.setSuc(false);
            response.setMsg("ConsumerDeRegisterRequest 不能为空");
            return response;
        }
        ConsumerEntity consumerEntity=get(request.getId());
        if(consumerEntity!=null){
            doDeleteConsumer(Arrays.asList(consumerEntity),1);
        }
        return response;
    }

//每个主题设置的最大熔断值
    protected Map<String, AtomicInteger> topicPerMax = new ConcurrentHashMap<>();
    //总的最大熔断值
    protected AtomicInteger totalMax = new AtomicInteger(0);


    /**
     * 1. 检查队列是否负载过大
     * 2. 检查能否插入进去
     * 3. 插入
     * @param request
     * @return
     */
    @Override
    public PublishMessageResponse publish(PublishMessageRequest request) {
        PublishMessageResponse response = new PublishMessageResponse();
        checkVaild(request, response);
        if (!response.isSuc()) {
            return response;
        }
        try {
            if (!checkTopicRate(request, response)) {
                return response;
            }
            Map<String, List<QueueEntity>> queueMap = queueService.getAllLocatedTopicWriteQueue();
            Map<String, List<QueueEntity>> topicQueueMap = queueService.getAllLocatedTopicQueue();
            if (queueMap.containsKey(request.getTopicName()) || topicQueueMap.containsKey(request.getTopicName())) {
                List<QueueEntity> queueEntities = queueMap.get(request.getTopicName());
                if (queueEntities == null || queueEntities.size() == 0) {
                    response.setSuc(false);
                    response.setMsg("topic_" + request.getTopicName() + "_and_has_no_queue!");
                    if (topicQueueMap.containsKey(request.getTopicName()) && soaConfig.getPublishMode() == 1) {
                        queueEntities = topicQueueMap.get(request.getTopicName());
                        updateQueueCache(request.getTopicName());
                    } else {
                        updateQueueCache(request.getTopicName());
                        return response;
                    }
                }
                if (queueEntities.size() > 0) {
                    saveMsg(request, response, queueEntities);
                }
            } else {
                response.setSuc(false);
                response.setMsg("topic1_" + request.getTopicName() + "_and_has_no_queue!");
                return response;
            }
        } catch (Exception e) {
            log.error("publish_error,and request json is " + JsonUtil.toJsonNull(request), e);
            response.setSuc(false);
            response.setMsg(e.getMessage());
        } finally {
            if (soaConfig.getEnableTopicRate() == 1) {
                totalMax.decrementAndGet();
                topicPerMax.get(request.getTopicName()).decrementAndGet();
            }
        }
        return response;
    }

    protected volatile long lastTime = 0;

    public void updateQueueCache(String topicName) {
        if (System.currentTimeMillis() - lastTime > 10 * 1000) {
            lastTime = System.currentTimeMillis();
            try {
                emailUtil.sendErrorMail(topicName + "没有可用的队列请注意", "没有可用的队列，请注意！！");
                queueService.resetCache();
                queueService.updateCache();
            } catch (Exception e) {

            }
        }
    }

    public boolean checkTopicRate(PublishMessageRequest request, PublishMessageResponse response) {
        // 关闭限速
        if (soaConfig.getEnableTopicRate() == 0) {
            return true;
        }
        if (!topicPerMax.containsKey(request.getTopicName())) {
            synchronized (this) {
                if (!topicPerMax.containsKey(request.getTopicName())) {
                    topicPerMax.put(request.getTopicName(), new AtomicInteger(0));
                }
            }
        }
        int totalMax1 = totalMax.incrementAndGet();
        int topicMax1 = topicPerMax.get(request.getTopicName()).incrementAndGet();
        if (soaConfig.getTopicFlag(request.getTopicName()).equals("0")) {
            response.setMsg(String.format("当前topic被设置为禁止发送topic", request.getTopicName()));
            response.setSleepTime(0);
            response.setCode(MqConstanst.YES);
            response.setSuc(false);
            return false;
        }
        if (soaConfig.getTopicHostMax() > 0 && totalMax1 > soaConfig.getTopicHostMax()) {
            response.setMsg(String.format("当前发送超过最大并发数了，需要降速,最大值为%s,当前值为%s", soaConfig.getTopicHostMax(), totalMax1));
            response.setSleepTime(Math.round(Math.random() * 1000));
            response.setCode(MqConstanst.NO);
            response.setSuc(false);
            return false;
        }
        int topicPer = soaConfig.getTopicPerMax(request.getTopicName());
        if (topicPer > 0 && topicMax1 > topicPer) {
            response.setMsg(String.format("当前topic发送超过最大并发数了，需要降速,最大值为%s,当前值为%s", topicPer, topicMax1));
            response.setSleepTime(Math.round(Math.random() * 1000));
            response.setCode(MqConstanst.NO);
            response.setSuc(false);
            return false;
        }
        return true;
    }

    @Override
    public PullDataResponse pullData(PullDataRequest request) {
        PullDataResponse response = new PullDataResponse();
        response.setSuc(true);
        Map<Long, QueueEntity> data = queueService.getAllQueueMap();
        checkVaild(request,response,data);
        if (!response.isSuc()) return response;

        QueueEntity temp = data.get(request.getQueueId());
        Map<Long, DbNodeEntity> dbNodeMap = dbNodeService.getCache();
        List<Message01Entity> entity=new ArrayList<>();
        if(checkFailTime(request.getTopicName(),temp,null)&&checkStatus(temp,dbNodeMap)){
            message01Service.setDbId(temp.getDbNodeId());

            try {
                entity = message01Service.getListDy(temp.getTopicName(), temp.getTbName(), request.getOffsetStart(), request.getOffsetEnd());
                dbFailMap.put(temp.getIp(),System.currentTimeMillis()-soaConfig.getDbFailWaitTime()*2000L);
            } catch (Exception e) {
                dbFailMap.put(temp.getIp(),System.currentTimeMillis());
            log.error("pulldate_search_error",e);}

        }
        List<MessageDto> messageDtos = convertMessageDto(entity);
        response.setMsgs(messageDtos);
        return response;
    }

    protected boolean checkFailTime(String topicName, QueueEntity entity, List<String> logLst) {
        if (dbFailMap.containsKey(entity.getIp())
                && (System.currentTimeMillis() - dbFailMap.get((entity.getIp()))) < soaConfig.getDbFailWaitTime()
                * 1000L) {
            if (logLst == null) {
                log.info("topicName_{}_queueid_{}_is_fail", topicName, entity.getId());
            }
            return false;
        }
        return true;
    }

    protected boolean checkStatus(QueueEntity temp, Map<Long, DbNodeEntity> dbNodeMap) {
        if (!dbNodeMap.containsKey(temp.getDbNodeId())) {
            return false;
        }
        if (dbNodeMap.get(temp.getDbNodeId()).getReadOnly() == 3) {
            return false;
        }
        return true;
    }

    @Override
    public FailMsgPublishAndUpdateResultResponse publishAndUpdateResultFailMsg(FailMsgPublishAndUpdateResultRequest request) {
        FailMsgPublishAndUpdateResultResponse response = new FailMsgPublishAndUpdateResultResponse();
        response.setSuc(true);
        QueueEntity queue = queueService.getAllQueueMap().get(request.getQueueId());
        if(request.getFailMsg()!=null){
            PublishMessageResponse publishMessageResponse = publish(request.getFailMsg());
            response.setSuc(publishMessageResponse.isSuc());
            //删除旧的失败消息
            if (request.getFailMsg().getMsgs() != null) {
                request.getFailMsg().getMsgs().forEach(t1 -> {
                    deleteOldFailMsg(request.getFailMsg(), t1, queue);
                });
            }
        }
        if(!CollectionUtils.isEmpty(request.getIds())){
            if(queue!=null&&queue.getNodeType()==2&&!CollectionUtils.isEmpty(request.getIds())){
                message01Service.setDbId(queue.getDbNodeId());
                message01Service.updateFailMsgResult(queue.getTbName(),request.getIds(),Message01Service.failMsgRetryCountSuc);
            }
        }
        return response;
    }

    private int deleteOldFailMsg(PublishMessageRequest request, ProducerDataDto t1, QueueEntity temp) {
        if (temp != null && temp.getNodeType() == 2 && temp.getTopicName().equals(request.getTopicName())) {
            message01Service.setDbId(temp.getDbNodeId());
            return message01Service.deleteOldFailMsg(temp.getTbName(), t1.getId(), t1.getRetryCount() - 1);
        }
        return 0;
    }

    @Override
    public GetMessageCountResponse getMessageCount(GetMessageCountRequest request) {
        GetMessageCountResponse response = new GetMessageCountResponse();
        response.setSuc(true);
        if (request == null || StringUtils.isEmpty(request.getConsumerGroupName())) {
            response.setSuc(false);
            response.setMsg("ConsumerGroupName不能为空！");
            return response;
        }
        Map<String, ConsumerGroupEntity> cache = consumerGroupService.getCache();
        if(!cache.containsKey(request.getConsumerGroupName())){
            response.setSuc(false);
            response.setMsg("ConsumerGroupName不能为空！");
            return response;
        }

        Map<String, Map<String, List<QueueOffsetEntity>>> map = queueOffsetService.getCache();
        List<QueueOffsetEntity> rs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getTopics())){
            for (String topic : request.getTopics()) {
                if (map.get(request.getConsumerGroupName()).containsKey(topic)) {
                    rs.addAll(map.get(request.getConsumerGroupName()).get(topic));
                }}
        }else {
                map.get(request.getConsumerGroupName()).values().forEach(rs::addAll);
            }

        List<Long> ids = rs.stream().map(QueueOffsetEntity::getId).collect(Collectors.toList());

        long offsetSum = queueOffsetService.getOffsetSumByIds(ids);
        long totalCount = 0;

        Map<Long, QueueEntity> queues = queueService.getAllQueueMap();
        for (QueueOffsetEntity offsetEntity : rs) {
            QueueEntity temp = queues.get(offsetEntity.getQueueId());
            message01Service.setDbId(temp.getDbNodeId());
            long maxId=queueService.getMaxId(temp.getId(),temp.getTbName());
            totalCount = totalCount + maxId - 1;
        }
        totalCount = totalCount - offsetSum;
        response.setCount(totalCount);
        return response;

    }

    @Override
    public int heartbeat(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            return consumerRepository.heartbeat(ids);
        }
        return 0;
    }

    @Override
    public List<ConsumerEntity> findByHeartTimeInterval(long heartTimeInterval) {
        return consumerRepository.findByHeartTimeInterval(heartTimeInterval);
    }

    @Override
    public boolean deleteByConsumers(List<ConsumerEntity> consumers) {
        if (CollectionUtils.isEmpty(consumers))
            return true;
        return doDeleteConsumer(consumers,0);
    }

    @Override
    public ConsumerEntity getConsumerByConsumerGroupId(Long consumerGroupId) {
        return consumerRepository.getConsumerByConsumerGroupId(consumerGroupId);

    }

    @Override
    public long countBy(Map<String, Object> conditionMap) {
        return consumerRepository.countBy(conditionMap);
    }

    @Override
    public List<ConsumerEntity> getListBy(Map<String, Object> conditionMap) {
        return consumerRepository.getListBy(conditionMap);
    }
    

    public void saveMsg(PublishMessageRequest request, PublishMessageResponse response, List<QueueEntity> queueEntities) {
        //<queueId,queue实体对象>
        Map<Long, QueueEntity> queueMap = queueEntities.stream().collect(Collectors.toMap(QueueEntity::getId, a -> a));

        //<traceId,分区信息,也就是一个queueId>
        Map<String, PartitionInfo> partitionMap = new HashMap<>();
        // <queueId,all message> 如果没有指定queueId,则使用最大值，不指定分区
        Map<Long, List<Message01Entity>> msgQueueMap = new HashMap<>();
        createMsg(request,msgQueueMap,partitionMap);

        for (Map.Entry<Long, List<Message01Entity>> listEntry : msgQueueMap.entrySet()) {
            Long queueId = listEntry.getKey();
            List<Message01Entity> message01Entities = listEntry.getValue();

            if (queueMap.containsKey(queueId)){
                doSaveMsg(request,response, Collections.singletonList(queueMap.get(queueId)),message01Entities);
            }else if (queueId==Long.MAX_VALUE){
                doSaveMsg(request, response, queueEntities, message01Entities);
            }else {
                for (Message01Entity message01Entity : message01Entities) {
                    if(partitionMap.containsKey(message01Entity.getTraceId())){
                        if(partitionMap.get(message01Entity.getTraceId()).getStrictMode()==0){
                            doSaveMsg(request,response,queueEntities, Collections.singletonList(message01Entity));
                        }
                    }
                }
            }

        }


    }

    private void createMsg(PublishMessageRequest request, Map<Long, List<Message01Entity>> queueMsg,
                           Map<String, PartitionInfo> partitionMap) {
        request.getMsgs().forEach(t1 -> {
            Message01Entity entity = new Message01Entity();
            entity.setBizId(t1.getBizId());
            entity.setBody(t1.getBody());
            entity.setHead(JsonUtil.toJson(t1.getHead()));
            entity.setRetryCount(t1.getRetryCount());
            entity.setTag(t1.getTag() + "");
            if (StringUtils.isEmpty(t1.getTraceId())) {
                t1.setTraceId(UUID.randomUUID().toString().replaceAll("-", ""));
            }
            entity.setTraceId(t1.getTraceId());
            entity.setSendIp(request.getClientIp());
            if (t1.getPartitionInfo() != null) {
                if (!queueMsg.containsKey(t1.getPartitionInfo().getQueueId())) {
                    queueMsg.put(t1.getPartitionInfo().getQueueId(), new ArrayList<>(10));
                }
                queueMsg.get(t1.getPartitionInfo().getQueueId()).add(entity);
                partitionMap.put(t1.getTraceId(), t1.getPartitionInfo());
            } else {
                if (!queueMsg.containsKey(Long.MAX_VALUE)) {
                    queueMsg.put(Long.MAX_VALUE, new ArrayList<>());
                }
                queueMsg.get(Long.MAX_VALUE).add(entity);
            }

        });

    }

    /**
     * 本段实现两个功能，一个是分配消息分配到哪个队列，一个是失败重发，并且添加远程log记录
     */
    private void doSaveMsg(PublishMessageRequest request, PublishMessageResponse response,
                           List<QueueEntity> queueEntities, List<Message01Entity> message01Entities) {
        int tryCount = 0;
        int queueSize = queueEntities.size();
        Exception last = null;
        String key = request.getTopicName();
        Map<String, AtomicInteger> counterTemp = counter.get();
        if (!counterTemp.containsKey(key)) {
            counterTemp.put(key, new AtomicInteger(0));
        }
        counterTemp.get(key).compareAndSet(Integer.MAX_VALUE, 0);
        int count = counterTemp.get(key).incrementAndGet();
        while (tryCount <= queueSize) {
            try {
                QueueEntity temp = queueEntities.get(count % queueEntities.size());
                count++;
                if (!checkFailTime(request.getTopicName(), temp, null)) {
                    continue;
                }
                doSaveMsg(message01Entities, request, response, temp);
                last = null;
                if (response.isSuc()) {
                    addPublishLog(message01Entities, request, MqConst.INFO, null);
                } else {
                    last = new RuntimeException(response.getMsg());
                }
                break;
            } catch (Exception e) {
                tryCount++;
                response.setSuc(false);
                response.setMsg("消息保存失败！");
                last = e;
            }
        }
        if (last != null) {
            addPublishLog(message01Entities, request, MqConst.ERROR, last);
            sendPublishFailMail(request, last, 2);
        }
    }

    @Resource
    EmailService emailService;
    public void sendPublishFailMail(PublishMessageRequest request, Exception last, int type) {
        if (soaConfig.enableSendFailTopicMail(request.getTopicName())) {
            SendMailRequest request2 = new SendMailRequest();
            request2.setServer(true);
            request2.setSubject("服务端,发送失败,topic:" + request.getTopicName());
            request2.setContent(last.getMessage() + " and request json is " + JsonUtil.toJsonNull(request)
                    + ",注意此邮件只是发给管理员注意情况,不代表消息发送最终失败,消息发送最终失败以客户端发送的邮件为准!");
            request2.setType(type);
            request2.setTopicName(request.getTopicName());
            request2.setKey("topic:" + request.getTopicName() + "-发送失败！");
            emailService.sendProduceMail(request2);
        }
    }

    private void addPublishLog(List<Message01Entity> message01Entities, PublishMessageRequest request, int info, Throwable th) {
        for (Message01Entity message01Entity : message01Entities) {
            LogDto logDto = new LogDto();
            logDto.setAction("message_publish");
            logDto.setBizId(message01Entity.getBizId());
            logDto.setTopicName(request.getTopicName());
            logDto.setTraceId(message01Entity.getTraceId());

            if (info==MqConst.ERROR){
                logDto.setThrowable(th);
                logDto.setAction("message_publish_erro");
                logDto.setMsg(JsonUtil.toJsonNull(message01Entity));
            }

            logDto.setType(info);
            logService.addBrokerLog(logDto);
        }
    }

    protected void doSaveMsg(List<Message01Entity> message01Entities, PublishMessageRequest request,
                             PublishMessageResponse response, QueueEntity temp){
        try {
            message01Service.setDbId(temp.getDbNodeId());
            message01Service.insertBatchDy(request.getTopicName(),temp.getTbName(),message01Entities);
            if (soaConfig.getMqPushFlag() == 1) {// apollo开关
                notifyClient(temp);
            }
            dbFailMap.put(getFailDbUp(temp), System.currentTimeMillis() - soaConfig.getDbFailWaitTime() * 2000L);
            response.setSuc(true);
        } catch (Exception e) {
            if (e instanceof DataIntegrityViolationException
                    || e.getCause() instanceof DataIntegrityViolationException) {
                response.setSuc(false);
                response.setMsg(e.getMessage());
                return;
            }
            dbFailMap.put(getFailDbUp(temp), System.currentTimeMillis());
            throw new RuntimeException(e);
        }
    }
    private final int timeout = 5000;
    private IHttpClient httpClient = new HttpClient(timeout, timeout);

    private void notifyClient(QueueEntity queueEntity) {
        Map<Long, List<QueueOffsetEntity>> queueIdQueueOffsetMap = queueOffsetService.getQueueIdQueueOffsetMap();
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        List<QueueOffsetEntity> queueOffsetList = queueIdQueueOffsetMap.get(queueEntity.getId());
        if (queueOffsetList == null) {
            return;
        }
        Map<String, List<MsgNotifyDto>> notifyMap = new HashMap<>();
        for (QueueOffsetEntity queueOffset : queueOffsetList) {
            // 如果消费者组开启了实时消息，则给对应的客户端发送异步通知。
            if (consumerGroupMap.get(queueOffset.getConsumerGroupName()).getPushFlag() == 1
                    && speedLimit(queueEntity.getId())){
                ConsumerUtil.ConsumerVo consumerVo = ConsumerUtil.parseConsumerId(queueOffset.getConsumerName());
                if (StringUtils.isEmpty(consumerVo.port)) {
                    continue;
                }
                String clienturl = "http://" + consumerVo.ip + ":" + consumerVo.port;
                if (!notifyMap.containsKey(clienturl)) {
                    notifyMap.put(clienturl, new ArrayList<>());
                }
                MsgNotifyDto msgNotifyDto = new MsgNotifyDto();
                msgNotifyDto.setConsumerGroupname(queueOffset.getConsumerGroupName());
                msgNotifyDto.setQueueId(queueEntity.getId());
                notifyMap.get(clienturl).add(msgNotifyDto);
            }
        }
        if (notifyMap.size()==0){
            return;
        }
        speedLimitMapRef.get().put(queueEntity.getId(), System.currentTimeMillis());
        for (String url : notifyMap.keySet()) {
            // 给对应的客户端发送拉取通知
            try {
                MsgNotifyRequest request = new MsgNotifyRequest();
                request.setMsgNotifyDtos(notifyMap.get(url));
                if (notifyFailTentativeLimit(url)) {
                    httpClient.postAsyn(url + "/mq/client/notify", request, new NotifyCallBack(url,notifyFailMapRef));
                }

            } catch (Exception e) {
                log.error("给客户端发送拉取通知异常：", e);
            }
        }


    }

    private boolean notifyFailTentativeLimit(String url) {
        NotifyFailVo notifyFailVo = notifyFailMapRef.get().get(url);
        if (notifyFailVo==null){
            return true;
        }
        if (notifyFailVo.isStatus()){
            return true;
        }
        //探测时间设定，若是httpClient超时设定为最小值
        int retryTime=Math.max(soaConfig.getMqNotifyFailTime(),timeout);
        if (System.currentTimeMillis() - notifyFailVo.getLastRetryTime() > retryTime) {
            // 处于重试失败状态
            // 如果已经有线程去试探了，直接返回
            if (notifyFailVo.getIsRetrying().get()) {
                return false;
            } else {// 否则试探一次
                if (notifyFailVo.getIsRetrying().compareAndSet(false, true)) {
                    notifyFailVo.setLastRetryTime(System.currentTimeMillis());
                    return true;
                } else {
                    return false;
                }
            }

        } else {
            return false;
        }
    }

    private boolean speedLimit(Long queueId) {
        Long lastTime = speedLimitMapRef.get().get(queueId);
        if (lastTime == null) {
            return true;
        }
//		System.out.println("差值："+(System.currentTimeMillis() - lastTime)+"----"+(System.currentTimeMillis() - lastTime > soaConfig.getMqClientNotifyTime()));

        if (System.currentTimeMillis() - lastTime > soaConfig.getMqClientNotifyTime()) {
            return true;
        } else {
            return false;
        }
    }

    private String getFailDbUp(QueueEntity temp) {
        return temp.getIp();
    }


    /**
     * 下线消费者
     * 触发重平衡
     * @param consumers 消费者
     * @param type 0 表示超时下线, 1表示系统下线
     */
    private boolean doDeleteConsumer(List<ConsumerEntity> consumers, int type) {
        boolean result =false;
        List<Long> consumerIds = consumers.stream().map(ConsumerEntity::getId).collect(Collectors.toList());
        List<Long> consumerGroupIds = new ArrayList<>(10);
        List<Long> broadConsumerGroupIds = new ArrayList<>(10);
        List<ConsumerGroupConsumerEntity> consumerGroupConsumers = consumerGroupConsumerService
                .getByConsumerIds(consumerIds);
        //评估下来, 不需要做cache,直接搜索
        Map<Long, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getIdCache();
        Map<String, ConsumerGroupEntity> consumerGroupNameMap = consumerGroupService.getCache();

        //判断是否是广播模式
        for (ConsumerGroupConsumerEntity consumerGroupConsumer : consumerGroupConsumers) {
            long consumerGroupId = consumerGroupConsumer.getConsumerGroupId();
            ConsumerGroupEntity groupEntity = consumerGroupMap.get(consumerGroupId);

            if (groupEntity!=null){
                if (groupEntity.getMode()==2&&!groupEntity.getOriginName().equals(groupEntity.getName())){
                    broadConsumerGroupIds.add(groupEntity.getId());
                    consumerGroupId=consumerGroupNameMap.get(groupEntity.getOriginName()).getId();
                }
            }

            consumerGroupIds.add(consumerGroupConsumer.getConsumerGroupId());
            if(type==0){
                log.warn("over{}s heart beats is unavailable, the consumer{} will be offline",
                        soaConfig.getConsumerInactivityTime(),
                        consumerGroupConsumer.getConsumerGroupId()+consumerGroupConsumer.getConsumerName());
            }else if (type==1){
                log.warn("the consumer{} will be offline",
                        consumerGroupConsumer.getConsumerGroupId()+consumerGroupConsumer.getConsumerName());
            }

        }
        deleteBroadConsumerGroup(broadConsumerGroupIds);
        doDeleteConsumerIds(consumerGroupConsumers,consumerIds,consumerGroupIds);
        result=true;
        return result;
    }

    /**
     * 删除consumer,queueoffset,consumerGroup by Id
     * 然后通知重平衡
     * @param consumerGroupConsumers consumer groups
     * @param consumerIds consumer id
     * @param consumerGroupIds consumer group Id
     */
    private void doDeleteConsumerIds(List<ConsumerGroupConsumerEntity> consumerGroupConsumers, List<Long> consumerIds, List<Long> consumerGroupIds) {
        consumerGroupConsumerService.deleteByConsumerIds(consumerIds);
        queueOffsetService.setConsumerIdsToNull(consumerIds);
        consumerRepository.batchDelete(consumerIds);

        //过滤Ip黑白名单
        Map<Long, ConsumerGroupEntity> cache = consumerGroupService.getIdCache();

        for (ConsumerGroupConsumerEntity consumerGroupConsumer : consumerGroupConsumers) {
            ConsumerGroupEntity consumerGroupEntity = cache.get(consumerGroupConsumer.getConsumerGroupId());
            if (consumerGroupEntity==null) continue;

            boolean blackIpFlag = !StringUtils.isEmpty(consumerGroupEntity.getIpBlackList()) && consumerGroupEntity.getIpBlackList().contains(consumerGroupConsumer.getIp());
            boolean whiteFlag = !StringUtils.isEmpty(consumerGroupEntity.getIpWhiteList()) && !consumerGroupEntity.getIpWhiteList().contains(consumerGroupConsumer.getIp());
            boolean notBlackIpFlag = !StringUtils.isEmpty(consumerGroupEntity.getIpBlackList()) &&! consumerGroupEntity.getIpBlackList().contains(consumerGroupConsumer.getIp());
            boolean notWhiteFlag = !StringUtils.isEmpty(consumerGroupEntity.getIpWhiteList()) && consumerGroupEntity.getIpWhiteList().contains(consumerGroupConsumer.getIp());

            if (blackIpFlag) {
                consumerGroupIds.remove(consumerGroupEntity.getId());
                log.warn("因为实例在黑名单中,所以不用重平衡{}", consumerGroupConsumer.getIp());
            }
            else if (whiteFlag){
                consumerGroupIds.remove(consumerGroupEntity.getId());
                log.warn("因为实例不在白名单中,所以不用重平衡{}", consumerGroupConsumer.getIp());
            }
            if(notBlackIpFlag){
                consumerGroupIds.add(consumerGroupEntity.getId());
                log.warn("需要重平衡{}",consumerGroupEntity.getId());
            }
            else if(notWhiteFlag){
                consumerGroupIds.add(consumerGroupEntity.getId());
                log.warn("需要重平衡{}",consumerGroupEntity.getId());
            }
        }
        consumerGroupService.notifyRb(consumerGroupIds);
    }

    private void deleteBroadConsumerGroup(List<Long> broadConsumerGroupIds) {
        if(CollectionUtils.isEmpty(broadConsumerGroupIds)) return;
        for (Long consumerGroupId : broadConsumerGroupIds) {
            consumerGroupService.deleteConsumerGroup(consumerGroupId, false);
        }
    }

    private void doRegisterConsumerGroup(ConsumerGroupRegisterRequest request, ConsumerGroupRegisterResponse response, ConsumerEntity consumerEntity) {
        response.setSuc(true);
        Map<String, ConsumerGroupEntity> consumerGroupMap = checkTopic(request, response);
        if (!response.isSuc()) return;

        List<ConsumerGroupConsumerEntity> consumerGroupConsumerEntities=new ArrayList<>();
        List<Long> ids=new ArrayList<>();
        ArrayList<String> consumerGroupNames = new ArrayList<>(request.getConsumerGroupNames().keySet());
        request.getConsumerGroupNames().keySet().forEach(t1->{
            if(!(","+consumerEntity.getConsumerGroupNames()+",").contains(","+t1+",")){
                if(StringUtils.isEmpty(consumerEntity.getConsumerGroupNames())){
                    consumerEntity.setConsumerGroupNames(t1);
                }else {
                    consumerEntity.setConsumerGroupNames(consumerEntity.getConsumerGroupNames()+","+t1);
                }
            }
            ConsumerGroupConsumerEntity consumerGroupConsumerEntity = new ConsumerGroupConsumerEntity();
            consumerGroupConsumerEntity.setConsumerGroupId(consumerGroupMap.get(t1).getId());
            consumerGroupConsumerEntity.setConsumerId(request.getConsumerId());
            consumerGroupConsumerEntity.setConsumerName(request.getConsumerName());
            consumerGroupConsumerEntity.setIp(request.getClientIp());
            consumerGroupConsumerEntities.add(consumerGroupConsumerEntity);
            ids.add(consumerGroupConsumerEntity.getConsumerGroupId());
        });
        doRegisterConsumerGroup(consumerEntity, consumerGroupConsumerEntities, ids, consumerGroupNames);
    }

    public Map<String, ConsumerGroupEntity> checkTopic(ConsumerGroupRegisterRequest request, ConsumerGroupRegisterResponse response) {
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService
                .getByNames(new ArrayList<>(request.getConsumerGroupNames().keySet()));
        if (consumerGroupMap.size() == 0) {
            response.setSuc(false);
            response.setMsg(String.join(".", request.getConsumerGroupNames().keySet()) + "不存在");
            return consumerGroupMap;
        }
        for (String name : request.getConsumerGroupNames().keySet()) {
            if(!consumerGroupMap.containsKey(name)){
                response.setSuc(false);
                response.setMsg("consumergroup_"+name+"不存在");
                return consumerGroupMap;
            }
            ConsumerGroupEntity entity = consumerGroupMap.get(name);
            String topicNames=","+entity.getTopicNames()+",";
            List<String> topics = request.getConsumerGroupNames().get(name);
            StringBuilder topicRs = new StringBuilder();
            for (String topicName : topics) {
                if (!topicNames.contains("," + topicName + ",")) {
                    // response.setSuc(false);
                    // response.setMsg(name + "下," + topicName + "不存在");
                    topicRs.append("客户端中，消费者组:").append(name).append("与topic:").append(topicName).append(
                            "的订阅关系，在后台管理界面中不存在。会出现客户端中此topic：").append(topicName).append("没有消息被消费！");
                    // return consumerGroupMap;
                } else {
                    while (topicNames.contains("," + topicName + ",")) {
                        topicNames = topicNames.replaceFirst("," + topicName + ",", ",");
                    }
                }
            }
            if (!StringUtils.isEmpty(topicNames.replaceAll(",", ""))) {
                // response.setSuc(false);
                // response.setMsg(entity.getName() + "下，" + topicNames + "没有被订阅！");
                topicRs.append("客户端中，消费者组").append(name).append("下,topic：").append(topicNames).append(
                        "在管理界面上订阅了，但是在客户端没有被订阅消费，请注意！会出现客户端topic：").append(topicNames).append("的消息不会被消费,产生堆积。");
                // return consumerGroupMap;
            }
        }
        return consumerGroupMap;
    }

    private void doRegisterConsumerGroup(ConsumerEntity consumerEntity,
                                         List<ConsumerGroupConsumerEntity> consumerGroupConsumerEntities,
                                         List<Long> ids, ArrayList<String> consumerGroupNames) {
        update(consumerEntity);
        registerConsumerGroupConsumer(consumerGroupConsumerEntities);
        Map<String, ConsumerGroupEntity> consumerGroupCacheMap = consumerGroupService.getCache();
        for (String consumerGroupName : consumerGroupNames) {
            ConsumerGroupEntity consumerGroupEntity = consumerGroupCacheMap.get(consumerGroupName);
            if (null==consumerGroupEntity) continue;
            if(!Util.isEmpty(consumerGroupEntity.getIpBlackList())&&consumerGroupEntity.getIpBlackList().contains(consumerEntity.getIp())){
                ids.remove(consumerGroupEntity.getId());
            } else if (!Util.isEmpty(consumerGroupEntity.getIpWhiteList())
                    && !consumerGroupEntity.getIpWhiteList().contains(consumerEntity.getIp())) {
                ids.remove(consumerGroupEntity.getId());
            }
        }
        consumerGroupService.notifyRb(ids);
    }

    public void registerConsumerGroupConsumer(List<ConsumerGroupConsumerEntity> consumerGroupConsumerEntities) {
        if (CollectionUtils.isEmpty(consumerGroupConsumerEntities)) {
            return;
        }
        try {
            consumerGroupConsumerService.insertBatch(consumerGroupConsumerEntities);
        } catch (Exception e) {
            consumerGroupConsumerEntities.forEach(t1 -> {
                try {
                    consumerGroupConsumerService.insert(t1);
                } catch (Exception e1) {
                    log.error("insertConsumserGroup_error",e);
                }
            });
        }
    }

    public void checkBroadcastAndSubEnv(ConsumerGroupRegisterRequest request,
                                         ConsumerGroupRegisterResponse response) {
        Map<String, ConsumerGroupEntity> map = consumerGroupService.getCache();
        if(request==null){
            response.setSuc(false);
            response.setMsg("参数不能为空");
            return;
        }
        if(request.getConsumerGroupNames()==null
        ||request.getConsumerGroupNames().size()==0){
            response.setSuc(false);
            response.setMsg("消费者组不能为空");
            return;
        }
        // 后续有删除操作，此处注意ConcurrentModificationException 异常
        List<String> consumerGroupNames= new ArrayList<>(request.getConsumerGroupNames().keySet());
        for (String name : consumerGroupNames) {
            if(!map.containsKey(name)){
                response.setSuc(false);
                response.setMsg("消费者组" + name + "不存在！");
                return;
            }
        }
        Map<Long, Map<String, ConsumerGroupTopicEntity>> gtopicMap=consumerGroupTopicService.getCache();
        for (Map.Entry<String, List<String>> t1 : request.getConsumerGroupNames().entrySet()) {
            if(map.containsKey(t1.getKey())){
                long cid = map.get(t1.getKey()).getId();
                StringBuilder builder = new StringBuilder();
                if(gtopicMap.containsKey(cid)){
                    t1.getValue().forEach(t2->{
                        if(!gtopicMap.get(cid).containsKey(t2)){
                            builder.append("客户端topic:[" + t2 + "]不在后台消费者组[" + t1.getKey() + "]订阅关系中，请注意！" + System.lineSeparator());
                        }
                    });
                    gtopicMap.get(cid).entrySet().forEach(t2 -> {
                        if (t2.getValue().getTopicType() == 1 && !t1.getValue().contains(t2.getKey())) {
                            builder.append("后台消费者组[" + t1.getKey() + "]中的topic:[" + t2.getKey() + "]不客户端订阅关系中，请注意！"
                                    + System.lineSeparator());
                        }
                    });
                }
                String content=builder.toString();
            }
        }
        checkBroadcastAndSubEnv(request, consumerGroupNames, map, response);

    }

    private void checkBroadcastAndSubEnv(ConsumerGroupRegisterRequest request,
                                         List<String> consumerGroupNames, Map<String, ConsumerGroupEntity> map,
                                         ConsumerGroupRegisterResponse response) {
        boolean flag=false;
        for (String name : consumerGroupNames) {
            ConsumerGroupEntity consumerGroupEntity = map.get(name);
            if (consumerGroupEntity.getMode()==2){
                String consumerGroupName =
                        ConsumerGroupUtil.getBroadcastConsumerName(consumerGroupEntity.getName(), request.getClientIp(),
                        request.getConsumerId());
                //创建消费者组
                ConsumerGroupEntity consumerGroupEntityNew = JsonUtil.copy(consumerGroupEntity,
                        ConsumerGroupEntity.class);
                consumerGroupEntityNew.setSubEnv(request.getSubEnv());
                consumerGroupEntityNew.setName(consumerGroupName);
                //替换掉consumerGroupName
                consumerGroupService.copyAndNewConsumerGroup(consumerGroupEntity,consumerGroupEntityNew);
                request.getConsumerGroupNames().put(consumerGroupEntityNew.getName(),request.getConsumerGroupNames().get(name));
                request.getConsumerGroupNames().remove(name);
                response.getBroadcastConsumerGroupName().put(name,consumerGroupEntityNew.getName());
                response.getConsumerGroupNameNew().put(name,consumerGroupEntityNew.getName());
            } else if (MqClient.getMqEnvironment()!=null
                    &&!Util.isEmpty(request.getSubEnv())&&!MqConst.DEFAULT_SUBENV.equalsIgnoreCase(request.getSubEnv()+"")
                    && MqEnv.FAT == MqClient.getMqEnvironment().getEnv()) {
                String newConsumerGroupName = consumerGroupEntity.getName() + "_" + request.getSubEnv().toLowerCase();
                ConsumerGroupEntity consumerGroupEntityNew = map.get(newConsumerGroupName);
                if (consumerGroupEntityNew == null) {
                    consumerGroupEntityNew = JsonUtil.copy(consumerGroupEntity, ConsumerGroupEntity.class);
                    consumerGroupEntityNew.setSubEnv(request.getSubEnv());
                    consumerGroupEntityNew.setName(newConsumerGroupName);
                    // consumerGroupEntityNew.setOriginName(newConsumerGroupName);
                    consumerGroupService.copyAndNewConsumerGroup(consumerGroupEntity, consumerGroupEntityNew);
                    request.getConsumerGroupNames().put(newConsumerGroupName, request.getConsumerGroupNames().get(name));
                    // 注意此时容易出现 ConcurrentModificationException 异常
                    request.getConsumerGroupNames().remove(name);
                    response.getBroadcastConsumerGroupName().put(name, newConsumerGroupName);
                    response.getConsumerGroupNameNew().put(name, consumerGroupEntityNew.getName());
                    flag=true;
                }
                if(flag){
                    consumerGroupService.forceUpdateCache();
                }else {
                    consumerGroupService.updateCache();
                }
            }
        }
    }

    private ConsumerEntity doRegisterConsumer(ConsumerRegisterRequest request) {
        ConsumerEntity consumerEntity = null;
        try {
            consumerEntity = new ConsumerEntity();
            consumerEntity.setName(request.getName());
            consumerEntity.setSdkVersion(request.getSdkVersion());
            consumerEntity.setLan(request.getLan());
            consumerEntity.setIp(request.getClientIp());
            consumerEntity.setHeartTime(new Date());
            consumerRepository.register(consumerEntity);
        } catch (Exception e) {
            Map<String,Object> condition= new HashMap<>();
            condition.put(ConsumerEntity.FdName,request.getName());
            consumerEntity = consumerRepository.get(condition);
        }
        return consumerEntity;
    }

    private void addRegisterLog(ConsumerRegisterRequest request) {
        LogDto logDto= new LogDto();
        logDto.setAction("is_register");
        logDto.setConsumerName(request.getName());
        logDto.setType(3);
        logService.addBrokerLog(logDto);
    }

    public void checkVaild(ConsumerRegisterRequest request, ConsumerRegisterResponse response) {
        if (request == null) {
            response.setSuc(false);
            response.setMsg("ConsumerRegisterRequest不能为空！");
            return;
        }
        if (StringUtils.isEmpty(request.getName())) {
            response.setSuc(false);
            response.setMsg("ConsumerName不能为空！");
            return;
        }
        if (StringUtils.isEmpty(request.getClientIp())) {
            response.setSuc(false);
            response.setMsg("Ip不能为空！");
            return;
        }
        if (StringUtils.isEmpty(request.getSdkVersion())) {
            response.setSuc(false);
            response.setMsg("SdkVersion不能为空！");
        }
    }
    protected void checkVaild(PublishMessageRequest request, PublishMessageResponse response) {
        response.setSuc(true);
        if (request == null) {
            response.setSuc(false);
            response.setMsg("request is null!");
            return;
        }
        if (CollectionUtils.isEmpty(request.getMsgs())) {
            response.setSuc(false);
            response.setMsg("topic_" + request.getTopicName() + "_msg_is_null!");
            return;
        }
        Map<String, TopicEntity> cacheData = topicService.getCache();
        if (!cacheData.containsKey(request.getTopicName())) {
            response.setSuc(false);
            response.setMsg("topic_" + request.getTopicName() + "_is_not_exist!");
            return;
        }
        if (!StringUtils.isEmpty(cacheData.get(request.getTopicName()).getToken())
                && !("" + cacheData.get(request.getTopicName()).getToken()).equals(request.getToken())) {
            response.setSuc(false);
            response.setMsg(
                    "topic_" + request.getTopicName() + "_and_token_" + request.getToken() + "_is_not_correct!");
            return;
        }
    }

    private void checkVaild(PullDataRequest request, PullDataResponse response, Map<Long, QueueEntity> data) {
        if (request == null) {
            response.setSuc(false);
            response.setMsg("参数不能为空！");
            return;

        }
        if (request.getQueueId() <= 0 || request.getOffsetStart() < 0
                || request.getOffsetStart() >= request.getOffsetEnd()) {
            response.setSuc(false);
            response.setMsg("参数不对！");
            return;
        }
        if (!data.containsKey(request.getQueueId())) {
            response.setSuc(false);
            response.setMsg("queueId_" + request.getQueueId() + "_is_not_exist！");
            return;
        }
    }

    private List<MessageDto> convertMessageDto(List<Message01Entity> entities) {
        List<MessageDto> messageDtos = new ArrayList<>(entities.size());
        entities.forEach(t1 -> {
            MessageDto messageDto = new MessageDto();
            messageDto.setBizId(t1.getBizId());
            messageDto.setBody(t1.getBody());
            messageDto.setHead(JsonUtil.parseJson(t1.getHead(), new TypeReference<Map<String, String>>() {
            }));
            messageDto.setId(t1.getId());
            messageDto.setRetryCount(t1.getRetryCount());
            messageDto.setTag(t1.getTag());
            messageDto.setTraceId(t1.getTraceId());
            messageDto.setSendTime(t1.getSendTime());
            messageDto.setSendIP(t1.getSendIp());
            messageDtos.add(messageDto);
        });
        return messageDtos;
    }
}
