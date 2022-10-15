package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.MqLockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MqLockRepository extends BaseRepository<MqLockEntity> {

    int updateHeartTimeByIdAndIp(@Param("id") long id,@Param("ip") String ip);
    int updateHeartTimeByKey1(@Param("ip") String ip, @Param("key1") String key1,
                              @Param("lockInterval") int lockInterval);

    int deleteOld(@Param("key1") String key1, @Param("lockInterval") int lockInterval);

    long insert1(MqLockEntity t);
}
