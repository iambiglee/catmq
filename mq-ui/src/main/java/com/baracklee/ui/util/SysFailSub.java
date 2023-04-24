package com.baracklee.ui.util;


import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.client.PublishMessageRequest;
import com.baracklee.mq.biz.event.ISubscriber;
import com.baracklee.mq.client.MqClient;

import java.util.List;

public class SysFailSub implements ISubscriber {

	@Override
	public List<Long> onMessageReceived(List<MessageDto> messages) {
		messages.forEach(message->{
			MqClient.getContext().getMqResource().publish(JsonUtil.parseJson(message.getBody(), PublishMessageRequest.class));
		});		
		return null;
	}
}
