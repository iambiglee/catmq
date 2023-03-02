package com.baracklee.mq.biz.dal.meta;


import com.baracklee.mq.biz.dal.common.BaseRepository;
import com.baracklee.mq.biz.entity.LastUpdateEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author dal-generator
 */
@Mapper
public interface TopicRepository extends BaseRepository<TopicEntity> {
	LastUpdateEntity getLastUpdate();

	//List<TopicEntity> getUpdateData(Date lastDate);

	TopicEntity getTopicByName(@Param("topicName")String topicName);

	List<TopicEntity> getListWithUserName(Map<String, Object> conditionMap);

	Long countWithUserName(Map<String, Object> conditionMap);

}
