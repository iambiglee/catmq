package com.baracklee;



import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;
import com.baracklee.mq.biz.event.ISubscriber;
import com.baracklee.mq.client.MqClient;

import java.time.LocalTime;
import java.util.List;

public class TestSub implements ISubscriber {
	@Override
	public List<Long> onMessageReceived(List<MessageDto> messages) {
		try {
			for (MessageDto message : messages) {
				System.out.println(LocalTime.now()+" "+message.getBody());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
