package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.EmailUtil;
import com.baracklee.mq.biz.common.util.SpringUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.Constants;
import com.baracklee.mq.biz.entity.NotifyMessageStatEntity;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.NotifyMessageStatService;
import com.baracklee.mq.biz.service.RedundanceCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Barack Lee
 */
@Component
public class RedundancyAllCheckService extends AbstractTimerService{

    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private SoaConfig soaConfig;
    private EmailUtil emailUtil;

    private NotifyMessageService notifyMessageService;

    private NotifyMessageStatService notifyMessageStatService;

    @Autowired
    public RedundancyAllCheckService(SoaConfig soaConfig,
                                     EmailUtil emailUtil,
                                     NotifyMessageService notifyMessageService,
                                     NotifyMessageStatService notifyMessageStatService) {
        this.soaConfig = soaConfig;
        this.emailUtil = emailUtil;
        this.notifyMessageService = notifyMessageService;
        this.notifyMessageStatService = notifyMessageStatService;
    }

    @PostConstruct
    private void init(){
        super.init(Constants.REDUNDANCE_CHECK, soaConfig.getCheckExpiredTime(), soaConfig);

        soaConfig.registerChanged(new Runnable() {
            private volatile int interval = soaConfig.getCheckExpiredTime();

            @Override
            public void run() {
                if (soaConfig.getCheckExpiredTime()!=interval){
                    interval=soaConfig.getCheckExpiredTime();
                    updateInterval(interval);
                }
            }
        });
    }


    public  String checkResult(){
        AtomicReference<String> result = new AtomicReference<>("");
        Map<String, RedundanceCheckService> startedServices = SpringUtil.getBeans(RedundanceCheckService.class);
        if (startedServices != null) {
            startedServices.entrySet().forEach(t1 -> {
                try {
                    String checkResult = null;
                    for (int retryConut = 0; retryConut < 3; retryConut++) {
                        try {
                            checkResult = t1.getValue().checkResult();
                            if (StringUtils.isEmpty(checkResult)) {
                                break;
                            } else {
                                Util.sleep(10000);
                            }
                        } catch (Exception e) {
                            checkResult = e.getMessage();
                            Util.sleep(10000);
                        }
                    }
                    if (!StringUtils.isEmpty(checkResult)) {
                        result.set(result.get() + checkResult);
                    }
                    log.info(t1.getKey() + "冗余校验启动完成！");
                } catch (Exception e) {
                    log.error(t1.getKey() + "启动异常！", e);
                }
            });

            String rs = checkNotifyMessageId();
            if (!StringUtils.isEmpty(rs)) {
                result.set(result.get() + rs);
            }
        }
        return  result.get();
    }

    @Override
    public void dostart() {
        String result = checkResult();
        if (!StringUtils.isEmpty(result)) {
            emailUtil.sendErrorMail("冗余字段校验", result, soaConfig.getAdminEmail());
        }

    }
    private String checkNotifyMessageId() {
        long maxId = notifyMessageService.getRbMaxId();
        NotifyMessageStatEntity notifyMessageStatEntity = notifyMessageStatService.get();
        if (maxId != 0 && notifyMessageStatEntity != null && notifyMessageStatEntity.getNotifyMessageId() > maxId) {
            notifyMessageStatService.updateNotifyMessageId();
            return "notifyMessage 表最大id为 " + maxId + ",notifyMessageStat表最大id为"
                    + notifyMessageStatEntity.getNotifyMessageId() + ",数据异常，已修复，请关注“mq client check meta data error” 错误邮件，如果出现请点击相关的消费者组然后点击重平衡即可！";
        }
        return "";
    }

    @Override
    @PreDestroy
    public void stopPortal() {
        super.stopPortal();
    }
}
