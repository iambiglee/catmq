package com.baracklee.mq.client.bootStrap;

import com.baracklee.mq.client.MqClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MqStartProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static AtomicBoolean initflag= new AtomicBoolean(false);
    private Environment env;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        if(env!=null){
            if(initflag.compareAndSet(false,true)){
                logger.info("消息客户端开始初始化！");
                MqClient.setSubscriberResolver(new SubscriberResolver());
                MqClientStartup.init(environment);
                logger.info("消息客户端初始化完成！");
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env=environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
