package com.baracklee.mq.biz.event;

public interface IAsynSubscriberSelector {
	IAsynSubscriber getSubscriber(String consumerGroupName, String topic);
}
