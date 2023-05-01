package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.Message01Service;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UiMessageService {
    private Message01Service message01Service;
    private QueueService queueService;

    private DbNodeService dbNodeService;
    private TopicService topicService;
    private SoaConfig soaConfig;

    @Autowired
    public UiMessageService(Message01Service message01Service,
                            QueueService queueService,
                            DbNodeService dbNodeService,
                            TopicService topicService,
                            SoaConfig soaConfig) {
        this.message01Service = message01Service;
        this.queueService = queueService;
        this.dbNodeService = dbNodeService;
        this.topicService = topicService;
        this.soaConfig = soaConfig;
    }



}
