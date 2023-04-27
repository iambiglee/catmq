package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.ServerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServerRepository extends BaseRepository<ServerEntity> {
    int deleteOld(@Param("heartTime") int heartTime);

    List<ServerEntity> getNoramlServer(@Param("heartTime") int heartTime);

    int updateHeartTimeById(@Param("id") long id);

    int insert1(ServerEntity entity);

    void batchUpdate(@Param("ids") List<Long> serverIds,@Param("statusFlag") int serverStatus);
}
