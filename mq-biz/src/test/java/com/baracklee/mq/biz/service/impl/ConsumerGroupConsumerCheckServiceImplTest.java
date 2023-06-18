package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.AbstractTest;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupConsumerEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupConsumerService;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.ConsumerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ConsumerGroupConsumerCheckServiceImplTest  extends AbstractTest {

	private ConsumerGroupService consumerGroupService;

	private ConsumerService consumerService;

	private ConsumerGroupConsumerService consumerGroupConsumerService;

	private ConsumerGroupConsumerCheckServiceImpl consumerGroupConsumerCheckServiceImpl;

	@Before
	public void init() {
		consumerGroupConsumerCheckServiceImpl = new ConsumerGroupConsumerCheckServiceImpl(consumerGroupService,consumerService,consumerGroupConsumerService);
		consumerGroupService = mock(ConsumerGroupService.class);
		consumerService = mock(ConsumerService.class);
		consumerGroupConsumerService = mock(ConsumerGroupConsumerService.class);
		ReflectionTestUtils.setField(consumerGroupConsumerCheckServiceImpl, "consumerGroupService", consumerGroupService);
		ReflectionTestUtils.setField(consumerGroupConsumerCheckServiceImpl, "consumerService",
				consumerService);
		ReflectionTestUtils.setField(consumerGroupConsumerCheckServiceImpl, "consumerGroupConsumerService", consumerGroupConsumerService);
	}
	
	@Test
	public void checkItemTest() {
		assertEquals(false, Util.isEmpty(consumerGroupConsumerCheckServiceImpl.checkItem()));
	}
	
	@Test
	public void checkResultTest() {
		Map<Long, ConsumerGroupEntity> consumerGroupMap=new HashMap<Long, ConsumerGroupEntity>();
		consumerGroupMap.put(2L, new ConsumerGroupEntity());
		List<ConsumerEntity> consumerList=new ArrayList<ConsumerEntity>();
		ConsumerEntity  consumerEntity=new ConsumerEntity();
	    consumerEntity.setId(2L);
	    consumerList.add(consumerEntity);
	    
	    
	    List<ConsumerGroupConsumerEntity> consumerGroupConsumerList=new ArrayList<ConsumerGroupConsumerEntity>();
		ConsumerGroupConsumerEntity consumerGroupConsumerEntity=new ConsumerGroupConsumerEntity();
		consumerGroupConsumerEntity.setId(1L);
		consumerGroupConsumerEntity.setConsumerGroupId(3L);
		consumerGroupConsumerList.add(consumerGroupConsumerEntity);
		
//		Map<Long, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getIdCache();
//        List<ConsumerEntity> consumerList =consumerService.getList();
//        List<ConsumerGroupConsumerEntity> consumerGroupConsumerList=consumerGroupConsumerService.getList();
		when(consumerGroupService.getIdCache()).thenReturn(consumerGroupMap);
		when(consumerService.getList()).thenReturn(consumerList);
		when(consumerGroupConsumerService.getList()).thenReturn(consumerGroupConsumerList);
		
		assertEquals(2, search(consumerGroupConsumerCheckServiceImpl.checkResult(),"<br/>"));
	}
}
