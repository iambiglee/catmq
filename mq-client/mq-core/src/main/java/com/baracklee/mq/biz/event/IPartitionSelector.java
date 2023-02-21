package com.baracklee.mq.biz.event;

import com.baracklee.mq.biz.dto.base.PartitionInfo;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;

import java.util.List;

public interface IPartitionSelector {
	//如果没有返回0
	PartitionInfo getPartitionId(String topic, ProducerDataDto message, List<Long> partitionIds);
}
