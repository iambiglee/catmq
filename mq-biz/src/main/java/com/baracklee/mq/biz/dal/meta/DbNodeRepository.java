package com.baracklee.mq.biz.dal.meta;


import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author dal-generator
 */
@Mapper
public interface DbNodeRepository extends BaseRepository<DbNodeEntity> {
	LastUpdateEntity getLastUpdate();

	//List<DbNodeEntity> getUpdateData(Date lastDate);

	List<DbNodeEntity> findConnStr();

}
