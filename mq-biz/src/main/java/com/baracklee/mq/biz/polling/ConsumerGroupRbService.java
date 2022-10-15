package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsumerGroupRbService extends AbstractTimerService{

    private Logger log = LoggerFactory.getLogger(ConsumerGroupRbService.class);
    @Autowired
    private SoaConfig soaConfig;


    @Resource
    ConsumerGroupService consumerGroupService;
    @Override
    public void dostart() {
        if(!soaConfig.isEnableRb()){return;}
        if(!checkNotityMessageStatId()){
            initNotifyMessageStatId();
        }
        if (lastMaster != isMaster()) {
            if (!checkNotifyMessageStatId()) {
                initNotifyMessageStatId();
            }
            lastMaster=isMaster();
        }
        long currentMaxId = getNotifyMessageId();
        if(currentMaxId==0){return;}

        List<ConsumerGroupEntity> consumerGroupEntities = consumerGroupService
                .getLastRbConsumerGroup(lastNotifyMessageId, currentMaxId);
        if (CollectionUtils.isEmpty(consumerGroupEntities)) {
            return;
        }

        Map<Long, ConsumerGroupQuqueVo> consumerGroupMap = new HashMap<>();
        initRbData(consumerGroupEntities, consumerGroupMap);
        for(ConsumerGroupQuqueVo t1: consumerGroupMap.values()){
            addRbCompleteLog(t1,t1.consumerGroup.getName() + "开始重平衡！__version_is_" + t1.consumerGroup.getVersion());
            rb(t1);
            for (int i = 0; i < 3; i++) {
                try {
                    if(isMaster()){
                        consumerGroupService.rb(t1.queueOffsets);
                    }
                    break;
                } catch (Exception e) {
                    log.error("doCheckRebalance_error", e);
                    Util.sleep(5000);
                }
            }
            addRbCompleteLog(t1,t1.consumerGroup.getName() + "重平衡完毕！__version_is_" + t1.consumerGroup.getVersion());
        }
        updateNotifyMessageId(currentMaxId);
        int count = consumerGroupConsumerService.deleteUnActiveConsumer();
        if (count > 0) {
            log.info("consumerGroupConsumer_empty,count is " + count);
        }
    }


}
