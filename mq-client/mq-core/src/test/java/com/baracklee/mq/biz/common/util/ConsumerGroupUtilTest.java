package com.baracklee.mq.biz.common.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ConsumerGroupUtilTest {

	@Test
	public void getBroadcastConsumerNameTest() {
		assertEquals("1_2-3", ConsumerGroupUtil.getBroadcastConsumerName("1", "2", 3));
	}
	
	
	@Test
	public void getOriginConsumerNameTest() {
		assertEquals("2_1", ConsumerGroupUtil.getOriginConsumerName("2_1_2-3"));
		assertEquals("2-3", ConsumerGroupUtil.getOriginConsumerName("2-3"));
	}
}
