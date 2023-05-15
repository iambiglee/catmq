package com.baracklee.ui.controller;

import com.baracklee.mq.biz.dto.request.ConsumerGroupCreateRequest;
import com.baracklee.mq.biz.dto.response.ConsumerGroupCreateResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupDeleteResponse;
import com.baracklee.mq.biz.dto.response.ConsumerGroupEditResponse;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.ui.dto.request.ConsumerGroupGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.*;
import com.baracklee.ui.service.UiConsumerGroupService;
import com.baracklee.ui.service.UiConsumerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Barack Lee
 */

@RestController
@RequestMapping("/consumerGroup")
public class ConsumerGroupController {

    private UiConsumerGroupService uiConsumerGroupService;

    private ConsumerGroupService consumerGroupService;

    public ConsumerGroupController(UiConsumerGroupService uiConsumerGroupService,
                                   ConsumerGroupService consumerGroupService) {
        this.uiConsumerGroupService = uiConsumerGroupService;
        this.consumerGroupService = consumerGroupService;
    }

    @RequestMapping("/list/data")
    public ConsumerGroupGetListResponse findBy(ConsumerGroupGetListRequest consumerGroupGetListRequest) {
        return uiConsumerGroupService.findBy(consumerGroupGetListRequest);
    }

    @RequestMapping("/createAndUpdate")
    public ConsumerGroupCreateResponse createConsumerGroup(ConsumerGroupCreateRequest consumerGroupCreateRequest) {
        return consumerGroupService.createConsumerGroup(consumerGroupCreateRequest);
    }

    @RequestMapping("/edit")
    public ConsumerGroupEditResponse editConsumerGroup(@RequestParam("ConsumerGroupEntity") ConsumerGroupEntity consumerGroupEntity) {
        return consumerGroupService.editConsumerGroup(consumerGroupEntity);
    }

    @RequestMapping("/delete")
    public ConsumerGroupDeleteResponse deleteConsumerGroup(long consumerGroupId) {
        return consumerGroupService.deleteConsumerGroup(consumerGroupId,true);
    }

    @GetMapping("/getById")
    public ConsumerGroupGetByIdResponse getById(Long id) {
        return uiConsumerGroupService.getById(id);
    }

    @RequestMapping("/refreshMeta")
    public ConsumerGroupRefreshMetaResponse refreshMeta(long consumerGroupId) {
        consumerGroupService.notifyMeta(consumerGroupId);
        return new ConsumerGroupRefreshMetaResponse();
    }

    @RequestMapping("/rebalence")
    public ConsumerGroupRebalenceResponse rebalence(long consumerGroupId) {
        consumerGroupService.notifyRb(consumerGroupId);
        return new ConsumerGroupRebalenceResponse();
    }

    @RequestMapping("/getConsumerGpNames")
    public ConsumerGroupGetNamesResponse getConsumerGpNames(String keyword, int offset, int limit){
        if (StringUtils.isEmpty(keyword)) {
            return new ConsumerGroupGetNamesResponse(0L, null);
        }
        return uiConsumerGroupService.getConsumerGpNames(keyword, offset,limit);
    }


    @RequestMapping("/consumerGroupSelect")
    public ConsumerGroupSelectResponse searchConsumerGroups(String keyword, int offset, int limit) {
        return uiConsumerGroupService.searchConsumerGroups(keyword,offset,limit);
    }

}
