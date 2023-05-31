package com.baracklee.rest.mq.controller.meta;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(MqConstanst.METAPRE)
public class MetaServerController {
    public static final String HTTP = "http://";
    private ServerService serverService;
    private SoaConfig soaConfig;
    private ConsumerGroupService consumerGroupService;
    private QueueService queueService;

    @Autowired
    public MetaServerController(ServerService serverService, SoaConfig soaConfig, ConsumerGroupService consumerGroupService, QueueService queueService) {
        this.serverService = serverService;
        this.soaConfig = soaConfig;
        this.consumerGroupService = consumerGroupService;
        this.queueService = queueService;
    }

    @PostMapping("/getMeta")
    public GetMetaResponse getMeta(@RequestBody GetMetaRequest request) {
        GetMetaResponse response = new GetMetaResponse();
        response.setSuc(true);
        response.setBrokerMetaMode(soaConfig.getBrokerMetaMode());
        response.setBrokerIp(serverService.getBrokerUrlCache());
        return response;
    }

    @PostMapping("/getMetaGroup")
    public GetMetaGroupResponse getMeta(@RequestBody GetMetaGroupRequest request) {
        GetMetaGroupResponse response = new GetMetaGroupResponse();
        response.setSuc(true);
        response.setBrokerMetaMode(soaConfig.getBrokerMetaMode());
        response.setMetricUrl(soaConfig.getMetricUrl());
        List<String> lstData = serverService.getBrokerUrlCache();
        int groupCount = soaConfig.getServerGroupCount();
        // if broker over 12, then, change the make 0-12 as unimportant
        response.setGroupFlag(lstData.size() > (groupCount * 3) ? 1 : 0);
        if (response.getGroupFlag() == 0) {
            response.setBrokerIpG1(lstData);
        } else {
            response.setBrokerIpG1(lstData.subList(groupCount, lstData.size()));
            addUrl(response);
            response.setBrokerIpG2(lstData.subList(0, groupCount));
        }
        return response;
    }

    private void addUrl(GetMetaGroupResponse response) {
        if (response.getBrokerIpG1().size() < soaConfig.getMinServerCount() && !Util.isEmpty(soaConfig.getBrokerDomain())) {
            List<String> rs = new ArrayList<>(soaConfig.getMinServerCount());
            rs.addAll(response.getBrokerIpG1());
            for (int i = rs.size(); i < soaConfig.getMinServerCount(); i++) {
                rs.add(HTTP + soaConfig.getBrokerDomain());
            }
            response.setBrokerIpG1(rs);

        }
    }

    @PostMapping("/getTopic")
    public GetTopicResponse getTopic(@RequestBody GetTopicRequest request) {
        GetTopicResponse response = new GetTopicResponse();
        response.setSuc(true);
        if (request != null && !StringUtils.isEmpty(request.getConsumerGroupName())) {
            ConsumerGroupEntity group = consumerGroupService.getCache().get(request.getConsumerGroupName());
            if (group != null) {
                response.setTopics(Arrays.asList(group.getTopicNames().split(",")));
            }
        }
        return response;
    }

    @PostMapping("/getGroupTopic")
    public GetGroupTopicResponse getGroupTopic(@RequestBody GetGroupTopicRequest request) {
        GetGroupTopicResponse response = new GetGroupTopicResponse();
        response.setSuc(true);
        List<GroupTopicDto> groupTopics = new ArrayList<>();
        response.setGroupTopics(groupTopics);
        if (request != null && !CollectionUtils.isEmpty(request.getConsumerGroupNames())) {
            request.getConsumerGroupNames().forEach(t1 -> {
                ConsumerGroupEntity group = consumerGroupService.getCache().get(t1);
                if (group != null) {
                    GroupTopicDto groupTopicDto = new GroupTopicDto();
                    groupTopicDto.setConsumerGroupName(t1);
                    groupTopicDto.setTopics(Arrays.asList(group.getTopicNames().split(",")));
                    groupTopics.add(groupTopicDto);
                }
            });

        }
        return response;
    }

    @PostMapping("/getTopicQueueIds")
    public GetTopicQueueIdsResponse getTopicQueueIds(@RequestBody GetTopicQueueIdsRequest request) {
        GetTopicQueueIdsResponse response = new GetTopicQueueIdsResponse();
        response.setSuc(true);
        if (request != null && !CollectionUtils.isEmpty(request.getTopicNames())) {
            Map<String, List<QueueEntity>> topicQueues = queueService.getAllLocatedTopicWriteQueue();
            Map<String, List<Long>> result = new HashMap<>();
            if (topicQueues != null) {
                request.getTopicNames().forEach(t1 -> {
                    if (topicQueues.containsKey(t1)) {
                        List<Long> ids = new ArrayList<>(topicQueues.get(t1).size());
                        topicQueues.get(t1).forEach(t2 -> {
                            ids.add(t2.getId());
                        });
                        result.put(t1, ids);
                    }
                });
            }
            response.setTopicQueues(result);
        }
        return response;
    }

}
