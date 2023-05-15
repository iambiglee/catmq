package com.baracklee.ui.controller;

import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicCreateRequest;
import com.baracklee.mq.biz.dto.request.ConsumerGroupTopicDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupTopicCreateResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupTopicService;
import com.baracklee.mq.biz.ui.dto.request.ConsumerGroupTopicGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicEditResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicGetByIdResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicGetListResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupTopicInitResponse;
import com.baracklee.ui.service.UiConsumerGroupTopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Barack Lee
 */
@RestController
@RequestMapping("/consumerGroupTopic")
public class ConsumerGroupTopicController {
    @Autowired
    private UiConsumerGroupTopicService uiConsumerGroupTopicService;
    @Autowired
    private ConsumerGroupTopicService consumerGroupTopicService;

    public ConsumerGroupTopicController(UiConsumerGroupTopicService uiConsumerGroupTopicService,
                                        ConsumerGroupTopicService consumerGroupTopicService) {
        this.uiConsumerGroupTopicService = uiConsumerGroupTopicService;
        this.consumerGroupTopicService = consumerGroupTopicService;
    }

    @RequestMapping("/list/data")
    public ConsumerGroupTopicGetListResponse findBy(ConsumerGroupTopicGetListRequest consumerGroupTopicGetListRequest) {
        return uiConsumerGroupTopicService.findBy(consumerGroupTopicGetListRequest);
    }

    @RequestMapping("/create")
    public ConsumerGroupTopicCreateResponse createConsumerGroupTopicAndFailTopic(ConsumerGroupTopicCreateRequest consumerGroupTopicCreateRequest) {
        return consumerGroupTopicService.subscribe(consumerGroupTopicCreateRequest);
    }

    @RequestMapping("/edit")
    public ConsumerGroupTopicEditResponse editConsumerGroupTopic(ConsumerGroupTopicEntity consumerGroupTopicEntity) {
        return uiConsumerGroupTopicService.editConsumerGroupTopic(consumerGroupTopicEntity);
    }

    /**
     * 取消订阅
     *
     * @param consumerGroupTopicId
     * @return
     */
    @RequestMapping("/delete")
    public ConsumerGroupTopicDeleteResponse deleteConsumerGroupTopic(Long consumerGroupTopicId) {
        return consumerGroupTopicService.deleteConsumerGroupTopic(consumerGroupTopicId);
    }

    @GetMapping("/getById")
    public ConsumerGroupTopicGetByIdResponse getById(Long consumerGroupTopicId) {
        return new ConsumerGroupTopicGetByIdResponse(uiConsumerGroupTopicService.findById(consumerGroupTopicId));
    }

    @GetMapping("/initConsumerGroupTopic")
    public ConsumerGroupTopicInitResponse initConsumerGroupTopic(Long consumerGroupId) {
        return new ConsumerGroupTopicInitResponse(uiConsumerGroupTopicService.initConsumerGroupTopic(consumerGroupId));
    }


}
