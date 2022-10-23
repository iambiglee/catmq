package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConsumerRepository extends BaseRepository<ConsumerEntity> {
    int heartbeat(@Param("ids") List<Long> ids);

    List<ConsumerEntity> findByHeartTimeInterval(@Param("heartTimeInterval") long heartTimeInterval);

    boolean deleteByConsumerId(@Param("consumerId") Long consumerId);

    long register(ConsumerEntity t);

    ConsumerEntity getConsumerByConsumerGroupId(@Param("consumerGroupId") Long consumerGroupId);

    long countBy(Map<String, Object> conditionMap);

    List<ConsumerEntity> getListBy(Map<String, Object> conditionMap);


}
