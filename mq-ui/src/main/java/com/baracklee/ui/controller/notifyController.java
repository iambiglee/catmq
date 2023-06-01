package com.baracklee.ui.controller;

import com.baracklee.mq.biz.ui.dto.response.MessageNotifyResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageStatNotifyResponse;
import com.baracklee.mq.biz.ui.dto.response.MessageUpdateNotifyResponse;
import com.baracklee.ui.service.UiNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Barack Lee
 */
@RestController
@RequestMapping("/notify")
public class notifyController {
    @Autowired
    private UiNotifyService uiNotifyService;
    Logger log = LoggerFactory.getLogger(QueueController.class);

    @RequestMapping("/list/data")
    public MessageNotifyResponse getNotifyMessageByPage(long page, long limit) {
        return uiNotifyService.getNotifyMessageByPage(page, limit);

    }

    @RequestMapping("/list/key")
    public MessageStatNotifyResponse getNotifyKeyByPage(long page, long limit) {
        return uiNotifyService.getNotifyKey(page, limit);

    }

    @RequestMapping("/update/id")
    public MessageUpdateNotifyResponse updateNotifyMessage(long id, long notifyMessageId) {
        uiNotifyService.updateNotifyMessageStat(id, notifyMessageId);
        return new MessageUpdateNotifyResponse();

    }
}
