package com.baracklee.mq.biz.dto.client;


import com.baracklee.mq.biz.dto.BaseRequest;

public class GetTopicRequest extends BaseRequest {

	private String consumerGroupName;

	public String getConsumerGroupName() {
		return consumerGroupName;
	}

	public void setConsumerGroupName(String consumerGroupName) {
		this.consumerGroupName = consumerGroupName;
	}
}
