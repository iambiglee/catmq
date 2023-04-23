package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueueExpansionAlarmService extends AbstractTimerService{

    private QueueService queueService;

    private SoaConfig soaConfig;

    @Autowired
    public QueueExpansionAlarmService(QueueService queueService, SoaConfig soaConfig) {
        this.queueService = queueService;
        this.soaConfig = soaConfig;
    }

    @PostConstruct
    private void init(){
        super.init("mq_queueExpansion_sk", 0, soaConfig);
        soaConfig.registerChanged(new Runnable() {
            private volatile int interval = soaConfig.getQueueExpansionCheckInterval();
            @Override
            public void run() {
                if (soaConfig.getQueueExpansionCheckInterval() != interval) {
                    interval = soaConfig.getQueueExpansionCheckInterval();
                    updateInterval(interval);
                }
            }
        });
    }
    @Override
    public void dostart() {
        int availableQueuesNum = 0;
        int availableFailQueuesNum = 0;
        List<QueueEntity> queueList = new ArrayList<>(queueService.getAllQueueMap().values());
        for (QueueEntity queueEntity : queueList) {
            if(queueEntity.getNodeType()==1 && queueEntity.getTopicId()==0){
                availableQueuesNum++;
            }else if (queueEntity.getNodeType()==0 && queueEntity.getTopicId()==0){
                availableFailQueuesNum++;
            }
        }
    }

    @PreDestroy
    public void stopPortal() {
        super.stopPortal();
    }

}
