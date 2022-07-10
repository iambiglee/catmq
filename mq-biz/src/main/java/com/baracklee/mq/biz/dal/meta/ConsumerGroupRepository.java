package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConsumerGroupRepository extends BaseRepository <ConsumerGroupEntity>{

    LastUpdateEntity getLastUpdate();
}
