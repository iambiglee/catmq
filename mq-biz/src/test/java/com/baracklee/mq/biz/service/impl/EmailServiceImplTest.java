package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.AbstractTest;
import com.baracklee.mq.biz.common.util.EmailUtil;
import com.baracklee.mq.biz.dto.client.SendMailRequest;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.TopicService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class EmailServiceImplTest extends AbstractTest {
	private EmailServiceImpl emailServiceImpl;
	private TopicService topicService;
	private ConsumerGroupService consumerGroupService;
	private EmailUtil emailUtil;
	
	@Before
	public void init() {
		emailServiceImpl = new EmailServiceImpl();
		object = emailServiceImpl;
		topicService = mockAndSet(TopicService.class);
		consumerGroupService=mockAndSet(ConsumerGroupService.class);
		emailUtil=mockAndSet(EmailUtil.class);		
		super.init();
	}
	
	@Test
	public void sendConsumerMailTest() {
		SendMailRequest sendMailRequest=new SendMailRequest();
		sendMailRequest.setConsumerGroupName("test");
		sendMailRequest.setTopicName("test");
		sendMailRequest.setType(1);
		sendMailRequest.setSubject("test");
		
		
		
		Map<String, ConsumerGroupEntity> map =new HashMap<String, ConsumerGroupEntity>();
		ConsumerGroupEntity consumerGroupEntity=new ConsumerGroupEntity();
		map.put(sendMailRequest.getConsumerGroupName(), consumerGroupEntity);
	    when(consumerGroupService.getCache()).thenReturn(map);
	    
	    ConsumerGroupTopicEntity topicEntity=new ConsumerGroupTopicEntity();
	    topicEntity.setAlarmEmails("fasfas@com");
	    when(consumerGroupService.getTopic(anyString(),anyString())).thenReturn(topicEntity);
	    
	    emailServiceImpl.sendConsumerMail(sendMailRequest);
	    verify(emailUtil).sendMail(anyString(),anyString(),anyListOf(String.class),anyInt());
	    
	    sendMailRequest=new SendMailRequest();
		sendMailRequest.setType(1);
		sendMailRequest.setSubject("test");
	    emailServiceImpl.sendConsumerMail(sendMailRequest);
	    verify(emailUtil,times(2)).sendMail(anyString(),anyString(),anyListOf(String.class),anyInt());
	}
	
	@Test
	public void sendProduceMailTest() {
		SendMailRequest sendMailRequest=new SendMailRequest();
		sendMailRequest.setConsumerGroupName("test");
		sendMailRequest.setTopicName("test");
		sendMailRequest.setType(1);
		sendMailRequest.setSubject("test");
		
		Map<String, TopicEntity> map = new HashMap<String, TopicEntity>();
		map.put(sendMailRequest.getTopicName(), new TopicEntity());
		when(topicService.getCache()).thenReturn(map);
		
		emailServiceImpl.sendProduceMail(sendMailRequest);
	}
}
