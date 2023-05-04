package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.polling.RedundancyAllCheckService;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.service.impl.*;
import com.baracklee.mq.biz.ui.dto.response.RedundanceCheckResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Barack Lee
 */
@Service
public class UiRedundanceCheckService {
    private SoaConfig soaConfig;

    private RedundancyAllCheckService redundanceCheckService;

    private ConsumerGroupService consumerGroupService;

    private TopicService topicService;

    private ConsumerGroupTopicService consumerGroupTopicService;

    private QueueService queueService;

    private QueueOffsetService queueOffsetService;

    private DbNodeService dbNodeService;

    private ConsumerService consumerService;

    private ConsumerGroupConsumerService consumerGroupConsumerService;

    private ConsumerGroupCheckServiceImpl consumerGroupCheckService;

    private ConsumerGroupTopicCheckServiceImpl consumerGroupTopicCheckService;

    private QueueCheckServiceImpl queueCheckService;

    private QueueOffsetCheckServiceImpl queueOffsetCheckService;

    private TopicCheckServiceImpl topicCheckService;

    @Autowired
    public UiRedundanceCheckService(SoaConfig soaConfig,
                                    RedundancyAllCheckService redundancyCheckService,
                                    ConsumerGroupService consumerGroupService,
                                    TopicService topicService,
                                    ConsumerGroupTopicService consumerGroupTopicService,
                                    QueueService queueService,
                                    QueueOffsetService queueOffsetService,
                                    DbNodeService dbNodeService,
                                    ConsumerService consumerService,
                                    ConsumerGroupConsumerService consumerGroupConsumerService,
                                    ConsumerGroupCheckServiceImpl consumerGroupCheckService,
                                    ConsumerGroupTopicCheckServiceImpl consumerGroupTopicCheckService,
                                    QueueCheckServiceImpl queueCheckService,
                                    QueueOffsetCheckServiceImpl queueOffsetCheckService,
                                    TopicCheckServiceImpl topicCheckService) {
        this.soaConfig = soaConfig;
        this.redundanceCheckService = redundancyCheckService;
        this.consumerGroupService = consumerGroupService;
        this.topicService = topicService;
        this.consumerGroupTopicService = consumerGroupTopicService;
        this.queueService = queueService;
        this.queueOffsetService = queueOffsetService;
        this.dbNodeService = dbNodeService;
        this.consumerService = consumerService;
        this.consumerGroupConsumerService = consumerGroupConsumerService;
        this.consumerGroupCheckService = consumerGroupCheckService;
        this.consumerGroupTopicCheckService = consumerGroupTopicCheckService;
        this.queueCheckService = queueCheckService;
        this.queueOffsetCheckService = queueOffsetCheckService;
        this.topicCheckService = topicCheckService;
    }


    public RedundanceCheckResponse checkAll(){
        String result = redundanceCheckService.checkResult();
        result+=checkIp();
        if(StringUtils.isEmpty(result)){
            result="数据正常！";
        }
        return new RedundanceCheckResponse(result);
    }

    /**
     * topic 下队列是否同步到同一台服务器上
     * @return
     */
    private String checkIp() {
        StringBuilder checkIpResult1=new StringBuilder();
        StringBuilder checkIpResult2=new StringBuilder();
        Map<String, List<QueueEntity>> topicQueueMap=queueService.getAllLocatedTopicQueue();
        for (String topicName:topicQueueMap.keySet()) {
            String ip=topicQueueMap.get(topicName).get(0).getIp();
            boolean rs=false;
            for (QueueEntity queue:topicQueueMap.get(topicName)) {
                if(!ip.equals(queue.getIp())){
                    rs=true;
                    break;
                }
            }
            if(!rs){
                if(topicQueueMap.get(topicName).get(0).getNodeType()==1){
                    checkIpResult1.append("topic : "+topicName+" 的队列分布在一台物理机上(物理机ip:"+ip+"),为了防止单点故障，请扩容。"+"<br/>");
                }else{
                    checkIpResult2.append("topic : "+topicName+" 的队列分布在一台物理机上(物理机ip:"+ip+"),为了防止单点故障，请扩容。"+"<br/>");
                }

            }
        }

        return checkIpResult1.toString()+"----------------------<br/>"+checkIpResult2.toString();

    }
}
