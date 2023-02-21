package com.baracklee.mq.client.bootStrap;

import com.baracklee.mq.client.MqClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class MqClientShutdownListener implements ApplicationListener<ContextClosedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(MqClientShutdownListener.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        try {
            MqClientStartup.close();
            MqClient.close();
            logger.info("注册退出！");
        } catch (Exception e) {
            logger.error("MqClient_close_error", e);
        }
    }
}
