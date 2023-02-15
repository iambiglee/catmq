package com.baracklee.mq.biz.dto.client;


import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.List;

public class GetGroupTopicResponse extends BaseResponse {

	private List<GroupTopicDto> groupTopics;

	public List<GroupTopicDto> getGroupTopics() {
		return groupTopics;
	}

	public void setGroupTopics(List<GroupTopicDto> groupTopics) {
		this.groupTopics = groupTopics;
	}
}
