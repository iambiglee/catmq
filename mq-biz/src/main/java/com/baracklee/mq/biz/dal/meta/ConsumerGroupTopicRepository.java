package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
@Mapper
public interface ConsumerGroupTopicRepository extends BaseRepository<ConsumerGroupTopicEntity> {

    void deleteByConsumerGroupId(@Param("consumerGroupId")long consumerGroupId);
    void deleteByOriginTopicName(@Param("consumerGroupId")long consumerGroupId,@Param("originTopicName") String originTopicName);
    List<String> getFailTopicNames(@Param("consumerGroupId")long consumerGroupId);
    ConsumerGroupTopicEntity getCorrespondConsumerGroupTopic(Map<String, Object> parameterMap);
    void updateEmailByGroupName(@Param("consumerGroupName") String groupName,@Param("alarmEmails") String alarmEmails);
    LastUpdateEntity getLastUpdate();
}
