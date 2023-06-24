package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.client.AbstractMockResource;
import com.baracklee.mq.client.AbstractTest;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * @Author： Barack Lee
 */
public class MqGroupExecutorServiceTest extends AbstractTest {

	@Test
    public void testRbOrUpdateInit()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MqGroupResource mqResource = new MqGroupResource();
        MqGroupExecutorService mqGroupExcutorService = new MqGroupExecutorService( mqResource);
        ConsumerGroupOneDto consumerGroupOne = buildModifyConsumerGroupOne();
        mqGroupExcutorService.rbOrUpdate(consumerGroupOne, "111");
        Field field = MqGroupExecutorService.class.getDeclaredField("localConsumerGroup");
        field.setAccessible(true);
        ConsumerGroupOneDto localConsumerGroupOneDto = (ConsumerGroupOneDto) field.get(mqGroupExcutorService);
        assertEquals("testRbOrUpdateAndNotStart error", JsonUtil.toJson(consumerGroupOne),
                JsonUtil.toJson(localConsumerGroupOneDto));


    }

    private static class MqGroupResource extends AbstractMockResource {
        private volatile int commitFlag = 0;

        @Override
        public void commitOffset(CommitOffsetRequest request) {
            commitFlag++;

        }

        public int getCommitFlag() {
            return commitFlag;
        }

        @Override
        public String getBrokerIp() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}