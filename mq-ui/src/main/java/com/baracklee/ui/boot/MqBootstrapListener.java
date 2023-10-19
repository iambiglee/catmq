package com.baracklee.ui.boot;

import com.baracklee.mq.biz.common.inf.PortalTimerService;
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

/**
 * @Author： Barack Lee
 */
@Component
public class MqBootstrapListener implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static boolean isInit= false;

    private ReportService reportService;

    @Autowired
    public MqBootstrapListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!isInit) {
            try {
                startTimer();
                startPortalTimer();
//                reportService.registerReport();
                isInit = true;
                log.info("mq init successfully！");
            } catch (Exception e) {
                log.error("mq初始化异常", e);
                throw e;
            }
        }

    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }


    private void startTimer(){
        Map<String, TimerService> beans = SpringUtil.getBeans(TimerService.class);
        if(!CollectionUtils.isEmpty(beans)){
            for (Map.Entry<String, TimerService> entry : beans.entrySet()) {
                entry.getValue().start();
                log.info(entry.getKey()+"start successfully");
            }
        }
    }

    private void startPortalTimer() {
        Map<String, PortalTimerService> startedServices = SpringUtil.getBeans(PortalTimerService.class);
        if (startedServices != null) {
            startedServices.entrySet().forEach(t1 -> {
                try {
                    t1.getValue().startPortal();
                    log.info(t1.getKey() + "启动完成！");
                } catch (Exception e) {
                    log.error(t1.getKey() + "启动异常！", e);
                }
            });
        }

    }


}
