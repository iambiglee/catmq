package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.AbstractTest;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import com.baracklee.mq.biz.service.TopicService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ConsumerGroupTopicCheckServiceImplTest extends AbstractTest {

	private ConsumerGroupTopicCheckServiceImpl consumerGroupTopicCheckServiceImpl;

	private ConsumerGroupTopicService consumerGroupTopicService;

	private ConsumerGroupService consumerGroupService;

	private TopicService topicService;

	private QueueOffsetService queueOffsetService;

	@Before
	public void init() {

		super.init();
		consumerGroupService = mock(ConsumerGroupService.class);

		consumerGroupTopicService = mock(ConsumerGroupTopicService.class);

		queueOffsetService = mock(QueueOffsetService.class);

		topicService = mock(TopicService.class);


		consumerGroupTopicCheckServiceImpl = new ConsumerGroupTopicCheckServiceImpl(consumerGroupTopicService,consumerGroupService,topicService,queueOffsetService,soaConfig);

	}

	@Test
	public void checkItemTest() {
		assertEquals(true, consumerGroupTopicCheckServiceImpl.checkItem() != null);
	}

	@Test
	public void checkResult1Test() {
		Map<String, ConsumerGroupTopicEntity> consumerGroupTopicMap = new HashMap<String, ConsumerGroupTopicEntity>();
		//t1.getConsumerGroupName() + "_" + t1.getTopicName()
		ConsumerGroupTopicEntity consumerGroupTopicEntity=new ConsumerGroupTopicEntity();
		consumerGroupTopicEntity.setConsumerGroupName("test");
		consumerGroupTopicEntity.setTopicName("testTopic");	
		consumerGroupTopicEntity.setTopicType(1);
		consumerGroupTopicEntity.setRetryCount(-1);
		consumerGroupTopicEntity.setDelayProcessTime(-1);
		consumerGroupTopicEntity.setTimeOut(-1);
		consumerGroupTopicMap.put(consumerGroupTopicEntity.getConsumerGroupName() + "_" + consumerGroupTopicEntity.getTopicName(), consumerGroupTopicEntity);
		
		when(consumerGroupTopicService.getGroupTopic()).thenReturn(consumerGroupTopicMap);
		
		String rs=consumerGroupTopicCheckServiceImpl.checkResult();
		assertEquals(10, search(rs, "<br/>"));
	}
	
	@Test
	public void checkResult2Test() {
		Map<String, ConsumerGroupTopicEntity> consumerGroupTopicMap = new HashMap<String, ConsumerGroupTopicEntity>();		
		//t1.getConsumerGroupName() + "_" + t1.getTopicName()
		ConsumerGroupTopicEntity consumerGroupTopicEntity=new ConsumerGroupTopicEntity();
		consumerGroupTopicEntity.setConsumerGroupName("test");
		consumerGroupTopicEntity.setTopicName("testTopic");	
		consumerGroupTopicEntity.setTopicType(1);
		consumerGroupTopicEntity.setRetryCount(soaConfig.getConsumerGroupTopicMaxRetryCount()+1);
		consumerGroupTopicEntity.setThreadSize(soaConfig.getConsumerGroupTopicMaxThreadSize()+1);
		consumerGroupTopicEntity.setPullBatchSize(soaConfig.getMaxPullBatchSize()+1);
		consumerGroupTopicEntity.setDelayProcessTime(soaConfig.getMaxDelayProcessTime()+1);
		consumerGroupTopicEntity.setMaxPullTime(soaConfig.getMaxDelayPullTime()+1);
		consumerGroupTopicMap.put(consumerGroupTopicEntity.getConsumerGroupName() + "_" + consumerGroupTopicEntity.getTopicName(), consumerGroupTopicEntity);
		
		when(consumerGroupTopicService.getGroupTopic()).thenReturn(consumerGroupTopicMap);
		// consumerGroupTopicService.getGroupTopic();
		Map<String, ConsumerGroupEntity> consumerGroupMap = new HashMap<String, ConsumerGroupEntity>();
		ConsumerGroupEntity consumerGroupEntity=new ConsumerGroupEntity();
		consumerGroupEntity.setName("test");
		consumerGroupEntity.setTopicNames("test");		
		consumerGroupMap.put(consumerGroupEntity.getName(), consumerGroupEntity);		
		when(consumerGroupService.getCache()).thenReturn(consumerGroupMap);
		Map<String, TopicEntity> topicMap = new HashMap<String, TopicEntity>();
		// topicService.getCache();
		Map<String, QueueOffsetEntity> unQueueOffset = new HashMap<String, QueueOffsetEntity>();
		unQueueOffset.put("fasf_fasf_dfasf", new QueueOffsetEntity());		
		when(queueOffsetService.getUqCache()).thenReturn(unQueueOffset);
		
		String rs=consumerGroupTopicCheckServiceImpl.checkResult();
		assertEquals(9, search(rs, "<br/>"));
	}
}
