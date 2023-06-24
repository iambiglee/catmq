package com.baracklee.mq.client.bootStrap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)

public class MqEnvPropTest {

    @Test
    public void getAllEnvTest() {
        //MapPropertySource
        ConfigurableEnvironment environment=mock(ConfigurableEnvironment.class);
        MutablePropertySources mutablePropertySources=new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(mutablePropertySources);
        Map<String, Object> map=new HashMap<String, Object>();
        map.put("test", "fa");
        map.put("password", "dfasf");
        environment.getPropertySources().addFirst(new MapPropertySource("test", map));
        MqEnvProp mqEnvProp=new MqEnvProp();
        mqEnvProp.setEnvironment(environment);
        assertEquals(2, mqEnvProp.getAllEnv().get("test").size());
    }
    @Test
    public void getEnv1Test() {
        //MapPropertySource
        ConfigurableEnvironment environment=mock(ConfigurableEnvironment.class);
        MutablePropertySources mutablePropertySources=new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(mutablePropertySources);
        Map<String, Object> map=new HashMap<String, Object>();
        map.put("test", "fa");
        map.put("password", "dfasf");
        environment.getPropertySources().addFirst(new MapPropertySource("test", map));
        MqEnvProp mqEnvProp=new MqEnvProp();
        mqEnvProp.setEnvironment(environment);
        assertEquals(2, mqEnvProp.getEnv().size());
    }

    @Test
    public void getEnvTest() {
        ConfigurableEnvironment environment=mock(ConfigurableEnvironment.class);
        MutablePropertySources mutablePropertySources=new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(mutablePropertySources);
        Map<String, Object> map=new HashMap<String, Object>();
        map.put("test.test2", "fa");
        map.put("test.password", "dfasf");
        environment.getPropertySources().addFirst(new MapPropertySource("test", map));
        MqEnvProp mqEnvProp=new MqEnvProp();
        mqEnvProp.setEnvironment(environment);
        assertEquals(2, mqEnvProp.getEnv("test").size());
    }

}