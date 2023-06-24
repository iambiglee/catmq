package com.baracklee.mq.client;

import com.baracklee.mq.client.stat.MqFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.Assert.assertEquals;
/**
*@Author： Barack Lee
*/

@RunWith(JUnit4.class)
public class MqBootstrapScanConfigTest {
    @Test
    public void test() {
        MqBootstrapScanConfig mqBootstrapScanConfig=new MqBootstrapScanConfig();
        FilterRegistrationBean filterRegistrationBean=mqBootstrapScanConfig.clientMqFilter(new MqFilter());
        assertEquals(1, filterRegistrationBean.getUrlPatterns().size());
    }
}