package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.MqLockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MqLockRepository extends BaseRepository<MqLockEntity> {

    int updateHeartTimeByIdAndIp(@Param("id") long id,@Param("ip") String ip);

}
