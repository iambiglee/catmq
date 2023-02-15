package com.baracklee.mq.biz.dto.client;


import com.baracklee.mq.biz.dto.BaseResponse;

import java.util.List;

public class GetTopicResponse extends BaseResponse {

	private List<String> topics;

	public List<String> getTopics() {
		return topics;
	}

	public void setTopics(List<String> topics) {
		this.topics = topics;
	}
}
