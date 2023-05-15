package com.baracklee.ui.controller;

import com.baracklee.mq.biz.ui.dto.request.ConsumerGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ConsumerDeleteResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGetListResponse;
import com.baracklee.ui.service.UiConsumerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Barack Lee
 */
@RestController
@RequestMapping("/consumer")
public class ConsumerController {

    private UiConsumerService uiConsumerService;

    public ConsumerController(UiConsumerService uiConsumerService) {
        this.uiConsumerService = uiConsumerService;
    }

    @RequestMapping("/list/data")
    public ConsumerGetListResponse getConsumerList(ConsumerGetListRequest consumerGetListRequest) {
        return uiConsumerService.getConsumerByPage(consumerGetListRequest);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ConsumerDeleteResponse deleteByTime(@RequestParam("consumerId") long consumerId) {
        return uiConsumerService.deleteByTime(consumerId);
    }

}
