package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.ConsumerUtil;
import com.baracklee.mq.biz.common.util.EmailUtil;
import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.Constants;
import com.baracklee.mq.biz.entity.ConsumerEntity;
import com.baracklee.mq.biz.service.ConsumerService;
import com.baracklee.mq.biz.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author： Barack Lee
 */
@Service
public class NoActiveConsumerService extends AbstractTimerService {

    private HttpClient httpClient = new HttpClient(1500, 1500);

    private final ConsumerService consumerService;
    private final SoaConfig soaConfig;

    private final EmailUtil emailUtil;

    private final DbService dbService;

    @Autowired
    public NoActiveConsumerService(ConsumerService consumerService,
                                   SoaConfig soaConfig,
                                   EmailUtil emailUtil,
                                   DbService dbService) {
        this.consumerService = consumerService;
        this.soaConfig = soaConfig;
        this.emailUtil = emailUtil;
        this.dbService = dbService;
    }

    @PostConstruct
    private void init(){
        super.init(Constants.NOACTIVE_CONSUMER,soaConfig.getConsumerCheckInterval(),soaConfig);
        soaConfig.registerChanged(new Runnable() {
            private volatile int interval = soaConfig.getConsumerCheckInterval();

            @Override
            public void run() {
                if (soaConfig.getConsumerCheckInterval()!=interval){
                    interval=soaConfig.getConsumerCheckInterval();
                    updateInterval(interval);
                }
            }
        });
    }


    @Override
    public void dostart() {
        List<ConsumerEntity> consumerList = consumerService.findByHeartTimeInterval(soaConfig.getConsumerInactivityTime());
        Date dbTime = dbService.getDbTime();
        if (CollectionUtils.isEmpty(consumerList)){
            return;
        }
        consumerList = doubleCheck(consumerList, dbTime);
        List<List<ConsumerEntity>> split = Util.split(consumerList,10);
        for (List<ConsumerEntity> consumerEntityList : split) {
            if (isMaster()){
                consumerService.deleteByConsumers(consumerEntityList);
            }
        }
    }

    private List<ConsumerEntity> doubleCheck(List<ConsumerEntity> consumerList, Date dbTime) {
        ArrayList<ConsumerEntity> rs = new ArrayList<>();
        for (ConsumerEntity consumerEntity : consumerList) {
            ConsumerUtil.ConsumerVo consumerVo = ConsumerUtil.parseConsumerId(consumerEntity.getName());
            if (Util.isEmpty(consumerVo.port)||(dbTime.getTime()-consumerEntity.getHeartTime().getTime())>soaConfig.getMaxConsumerNoActiveTime()){
                rs.add(consumerEntity);
            }else {
                String url = String.format("http://%s:%s/mq/client/hs", consumerVo.ip, consumerVo.port);
                if (!httpClient.check(url)) {
                    rs.add(consumerEntity);
                    String message = String.format("Consumer心跳异常，但是健康检查没有问题，请注意。url is %s,最后心跳时间为%s,当前时间为%s", url,
                            Util.formateDate(consumerEntity.getHeartTime()), Util.formateDate(dbTime));
                    emailUtil.sendWarnMail("NoActiveConsumerService", message);
                }
            }

        }
        return rs;
    }


    @PreDestroy
    @Override
    public void stopPortal() {
        super.stopPortal();
    }

}
