package com.baracklee.mq.biz.dto.client;


import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;

public class GetGroupTopicRequest extends BaseRequest {
	private List<String> consumerGroupNames;

	public List<String> getConsumerGroupNames() {
		return consumerGroupNames;
	}

	public void setConsumerGroupNames(List<String> consumerGroupNames) {
		this.consumerGroupNames = consumerGroupNames;
	}
}
