package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConsumerGroupRepository extends BaseRepository <ConsumerGroupEntity>{

    LastUpdateEntity getLastUpdate();

    List<ConsumerGroupEntity> getByNames(List<String> names);

    void updateRbVersion(List<Long> ids);
}
