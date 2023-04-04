package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupTopicCreateResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.common.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ConsumerGroupTopicService extends BaseService<ConsumerGroupTopicEntity> {
    /*
     * key为consumergroupid，内层key为topicname
     * */
    Map<Long,Map<String, ConsumerGroupTopicEntity>> getCache();
    void deleteByConsumerGroupId(long consumerGroupId);
    void deleteByOriginTopicName(long consumerGroupId,String originTopicName);
    List<String> getFailTopicNames(long consumerGroupId);
    ConsumerGroupTopicEntity getCorrespondConsumerGroupTopic(Map<String, Object> parameterMap);
    Map<String, ConsumerGroupTopicEntity> getGroupTopic();
    void updateCache();
    void updateEmailByGroupName(String groupName,String alarmEmails);
    ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest);
    ConsumerGroupTopicCreateResponse subscribe(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest,Map<String, ConsumerGroupEntity> consumerGroupMap) ;
    ConsumerGroupTopicDeleteResponse deleteConsumerGroupTopic(long consumerGroupTopicId);
    ConsumerGroupTopicEntity createConsumerGroupTopic(
            ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest);

    Map<String, List<ConsumerGroupTopicEntity>> getTopicSubscribeMap();
}
