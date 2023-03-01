package com.baracklee.controller.client;

import com.baracklee.MqConstanst;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.client.*;
import com.baracklee.mq.biz.service.ConsumerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping(MqConstanst.CONSUMERPRE)
public class ConsumerController {

    @Resource
    private ConsumerService consumerService;

    @Resource
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
    public PublishMessageResponse publish(@RequestBody PublishMessageRequest request){
//        return consumerService.deRegister(request);
    }

}
