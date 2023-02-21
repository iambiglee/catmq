package com.baracklee.mq.biz.event;


import com.baracklee.mq.biz.dto.base.ProducerDataDto;

//消息发送前事件
public interface PreSendListener {
	void onPreSend(ProducerDataDto producerDataDto);
}
