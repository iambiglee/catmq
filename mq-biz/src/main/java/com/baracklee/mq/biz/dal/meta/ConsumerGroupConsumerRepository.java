package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConsumerGroupConsumerRepository extends BaseRepository<ConsumerGroupConsumerEntity> {
    int deleteUnActiveConsumer();

    List<ConsumerGroupConsumerEntity> getByConsumerGroupIds(List<Long> consumerGroupIds);

}
