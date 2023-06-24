package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.AbstractTest;
import com.baracklee.mq.client.core.IMqGroupExecutorService;
import com.baracklee.mq.client.resource.IMqResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)
public class ConsumerPollingServiceTest extends AbstractTest {

    static long sleepTime = 1000L;

    @Test
    public void startTest()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        IMqResource mqPollingResource = mock(IMqResource.class);

        ConsumerPollingService consumerPollingService = new ConsumerPollingService(mqPollingResource);
        consumerPollingService.start();
        Util.sleep(sleepTime + 1000L);
        Field field = ConsumerPollingService.class.getDeclaredField("mqExcutors");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, IMqGroupExecutorService> mqExcutors = (Map<String, IMqGroupExecutorService>) (field
                .get(consumerPollingService));
        IMqGroupExecutorService iMqGroupExecutorService = mqExcutors.get(consumerGroupName);
        Util.sleep(sleepTime + 2000L);
       consumerPollingService.close();
    }
    @Test
    public void rbStartTest()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        IMqResource mqPollingResource = mock(IMqResource.class);
        ConsumerPollingService consumerPollingService = new ConsumerPollingService(mqPollingResource);
        consumerPollingService.start();

        Util.sleep(sleepTime + 1000L);
        consumerPollingService.close();
    }

}