package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.NotifyMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotifyMessageRepository extends BaseRepository<NotifyMessageEntity> {
    Long getMaxId(@Param("maxId1") long maxId1,  @Param("message_type") int messageType);

    Long getMaxId1(@Param("message_type") int messageType);

    int clearOld(@Param("clearOldTime") Long clearOldTime, @Param("id") long id);

    Long getMinId(@Param("message_type") int messageType);

    Long getMinId1();
}
