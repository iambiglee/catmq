package com.baracklee.rest.mq.boot;


import com.baracklee.mq.biz.cache.ConsumerGroupCacheService;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.ConsumerGroupChangedListener;
import com.baracklee.mq.biz.common.inf.TimerService;
import com.baracklee.mq.biz.common.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.invoke.MethodHandles;
import java.util.Map;

@Component
public class MqBootstrapListener implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private static final Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static boolean isInit = false;

    private ConsumerGroupCacheService consumerGroupCacheService;

    private ReportService reportService;

    @Autowired
    public MqBootstrapListener(ConsumerGroupCacheService consumerGroupCacheService, ReportService reportService) {
        this.consumerGroupCacheService = consumerGroupCacheService;
        this.reportService = reportService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isInit) {
            try {
                startTimer();
                startBrokeTimer();
                registerEvent();
                reportService.registerReport();
                isInit = true;
                log.info("mq init successfully！");
            } catch (Exception e) {
                log.error("mq初始化异常", e);
                throw e;
            }
        }

    }

    private void startTimer() {
        Map<String, TimerService> startedServices = SpringUtil.getBeans(TimerService.class);
        if (startedServices != null) {
            startedServices.forEach((key, value) -> {
                try {
                    value.start();
                    log.info(key + "启动完成！");
                } catch (Exception e) {
                    log.error(key + "启动异常！", e);
                }
            });
        }
    }

    private void startBrokeTimer() {
        Map<String, BrokerTimerService> beans = SpringUtil.getBeans(BrokerTimerService.class);
        if(CollectionUtils.isEmpty(beans)) return;

        for (Map.Entry<String, BrokerTimerService> timerServiceEntry : beans.entrySet()) {
            try {
                timerServiceEntry.getValue().startBroker();
                log.info(timerServiceEntry.getKey()+"启动完成");
            } catch (Exception e) {
                log.error(timerServiceEntry.getKey()+"启动异常",e);
            }
        }

    }
    private void registerEvent() {
        Map<String, ConsumerGroupChangedListener> dataMap = SpringUtil.getBeans(ConsumerGroupChangedListener.class);
        if (dataMap != null) {
            dataMap.entrySet().forEach(t1 -> {
                try {
                    consumerGroupCacheService.addListener(t1.getValue());
                    log.info(t1.getKey() + "注册成功！");
                } catch (Exception e) {
                    log.error(t1.getKey() + "注册异常！", e);
                }
            });
        }
    }
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
