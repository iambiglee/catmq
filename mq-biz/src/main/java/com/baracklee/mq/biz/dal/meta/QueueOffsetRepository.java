package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface QueueOffsetRepository extends BaseRepository<QueueOffsetEntity> {


    void updateConsumerId(QueueOffsetEntity t1);

    List<QueueOffsetEntity> getByConsumerGroupIds(ArrayList<Long> consumerGroupIds);
}
