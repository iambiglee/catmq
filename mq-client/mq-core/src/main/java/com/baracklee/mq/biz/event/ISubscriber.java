package com.baracklee.mq.biz.event;


import com.baracklee.mq.biz.dto.base.MessageDto;

import java.util.List;

public interface ISubscriber {
	List<Long> onMessageReceived(List<MessageDto> messages);
}
