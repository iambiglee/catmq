package com.baracklee.mq.biz.dal.meta;

import com.baracklee.mq.biz.dto.MetaCompareVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface DbRepository {
    Date getDbTime();
    Map<String, String> getMaxConnectionsCount();
    Integer getConnectionsCount();
    List<MetaCompareVo> getMetaCompareData(@Param("metaCompareSql") String metaCompareSql);



}
