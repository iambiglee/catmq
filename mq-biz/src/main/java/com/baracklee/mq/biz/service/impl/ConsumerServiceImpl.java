package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.MqConst;
import com.baracklee.mq.biz.MqEnv;
import com.baracklee.mq.biz.common.util.ConsumerGroupUtil;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.ConsumerRepository;
import com.baracklee.mq.biz.dto.LogDto;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.client.MqClient;
import com.sun.xml.internal.ws.policy.EffectiveAlternativeSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
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
        return response;
    }

    @Override
    public List<ConsumerGroupConsumerEntity> getConsumerGroupByConsumerGroupIds(ArrayList<Long> consumerGroupIds) {
        if(CollectionUtils.isEmpty(consumerGroupIds)){
            return new ArrayList<>();
        }
        return consumerGroupConsumerService.getByConsumerGroupIds(consumerGroupIds);
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

    private Map<String, ConsumerGroupEntity> checkTopic(ConsumerGroupRegisterRequest request, ConsumerGroupRegisterResponse response) {
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

    private void registerConsumerGroupConsumer(List<ConsumerGroupConsumerEntity> consumerGroupConsumerEntities) {
        if (CollectionUtils.isEmpty(consumerGroupConsumerEntities)) {
            return;
        }
        consumerGroupConsumerService.insertBatch(consumerGroupConsumerEntities);
    }

    private void checkBroadcastAndSubEnv(ConsumerGroupRegisterRequest request,
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
        logDto.setType("3");
        logService.addBrokerLog(logDto);
    }

    private void checkVaild(ConsumerRegisterRequest request, ConsumerRegisterResponse response) {
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
}
