package com.baracklee.mq.client.stat;

import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author： Barack Lee
 */
public class MqClientStatControllerTest {
    final String MQ_CLINET_STAT_OPEN = "mq.client.stat.open";

    @Test
    public void test() {
        Environment mock = mock(Environment.class);
        when(mock.getProperty(MQ_CLINET_STAT_OPEN, "true")).thenReturn("true");
        MqClientStatController mqClientStatController=new MqClientStatController();
        ReflectionTestUtils.setField(mqClientStatController,"env",mock);




        mqClientStatController.th();

    }
}