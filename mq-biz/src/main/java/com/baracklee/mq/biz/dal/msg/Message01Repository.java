package com.baracklee.mq.biz.dal.msg;

import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.Message01Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface Message01Repository extends BaseRepository<Message01Entity> {
    Message01Entity getMaxIdMsg(@Param("tbName") String s);

    long getMaxId(String dbName, String tbName);
}
