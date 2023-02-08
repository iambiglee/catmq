package com.baracklee.mq.biz.event;



import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.MessageDto;

import java.util.List;

public interface IAsynSubscriber {
	void onMessageReceived(List<MessageDto> messages, ConsumerQueueDto consumerQueue);
}
