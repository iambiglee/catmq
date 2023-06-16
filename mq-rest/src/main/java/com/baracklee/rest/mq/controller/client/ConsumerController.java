package com.baracklee.rest.mq.controller.client;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MqConstanst.CONSUMERPRE)
public class ConsumerController {

    @Autowired
    public ConsumerController(ConsumerService consumerService, SoaConfig soaConfig) {
        this.consumerService = consumerService;
        this.soaConfig = soaConfig;
    }

    private final ConsumerService consumerService;
    private SoaConfig soaConfig;

    @PostMapping("/register")
    public ConsumerRegisterResponse register(@RequestBody ConsumerRegisterRequest request){
        ConsumerRegisterResponse response = consumerService.register(request);
        return response;
    }

    @PostMapping("/registerConsumerGroup")
    public ConsumerGroupRegisterResponse consumerGroupRegister(@RequestBody ConsumerGroupRegisterRequest request){
        ConsumerGroupRegisterResponse response=consumerService.registerConsumerGroup(request);
        return response;
    }

    @PostMapping("/deRegister")
    public ConsumerDeRegisterResponse deRegister(@RequestBody ConsumerDeRegisterRequest request) {
        ConsumerDeRegisterResponse response = consumerService.deRegister(request);
        return response;
    }
    @PostMapping("/publish")
    public PublishMessageResponse publish(@RequestBody PublishMessageRequest request){
//        return consumerService.deRegister(request);
        return consumerService.publish(request);
    }

    @PostMapping("pullData")
    public PullDataResponse pulldata(@RequestBody PullDataRequest request ){
        PullDataResponse pullDataResponse = consumerService.pullData(request);
        return pullDataResponse;
    }

    @PostMapping("/publishAndUpdateResultFailMsg")
    public FailMsgPublishAndUpdateResultResponse publishAndUpdateResultFailMsg(
            @RequestBody FailMsgPublishAndUpdateResultRequest request) {
        FailMsgPublishAndUpdateResultResponse response = consumerService.publishAndUpdateResultFailMsg(request);
        return response;

    }

    @PostMapping("/getMessageCount")
    public GetMessageCountResponse getMessageCount(@RequestBody GetMessageCountRequest request) {
        GetMessageCountResponse response = consumerService.getMessageCount(request);
        return response;
    }
}
