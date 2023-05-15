package com.baracklee.ui.controller;


import com.baracklee.mq.biz.ui.dto.request.MqLockGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.MqLockDeleteResponse;
import com.baracklee.mq.biz.ui.dto.response.MqLockGetListResponse;
import com.baracklee.ui.service.UiMqLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/lock")
public class MqLockController {
    @Autowired
    private UiMqLockService uiMqLockService;
    Logger log = LoggerFactory.getLogger(ConsumerGroupController.class);

    @RequestMapping("/list/data")
    public MqLockGetListResponse findBy(MqLockGetListRequest baseUiRequst) {
        return uiMqLockService.findBy(baseUiRequst);
    }

    @RequestMapping("/delete")
    public MqLockDeleteResponse deleteLock(String lockId) {
        return uiMqLockService.delete(lockId);
    }

}
