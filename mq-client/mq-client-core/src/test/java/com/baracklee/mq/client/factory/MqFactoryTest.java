package com.baracklee.mq.client.factory;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)

public class MqFactoryTest {

    @Test
    public void test() {

        MqFactory mqFactory = new MqFactory();
        boolean rs = true;
        try {
            mqFactory.createMqBrokerUrlRefreshService();
        } catch (Exception e) {
            rs = false;
        }
        assertEquals("createMqBrokerUrlRefreshService error", true, rs);

        rs = true;
        try {
            mqFactory.createMqBrokerUrlRefreshService();
        } catch (Exception e) {
            rs = false;
        }
        assertEquals("createMqBrokerUrlRefreshService error", true, rs);
        mqFactory.createMqBrokerUrlRefreshService();
        mqFactory.createMqCheckService();
        mqFactory.createConsumerPollingService();
        mqFactory.createMqGroupExecutorService();
        mqFactory.createMqHeartbeatService();
        mqFactory.createMqMeticReporterService();
        mqFactory.createMqQueueExcutorService("ttt", new ConsumerQueueDto());
        mqFactory.createMqTopicQueueRefreshService();
    }
}
