package com.baracklee.controller.client;

import com.baracklee.MqConstanst;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterResponse;
import com.baracklee.mq.biz.service.ConsumerService;
import org.apache.ibatis.transaction.Transaction;
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

    @PostMapping("/register")
    public ConsumerRegisterResponse register(@RequestBody ConsumerRegisterRequest request){
        ConsumerRegisterResponse response = consumerService.register(request);
        return response;
    }

    public ConsumerGroupRegisterResponse consumerGroupRegister(@RequestBody ConsumerGroupRegisterRequest request){
        ConsumerGroupRegisterResponse response=consumerService.registerConsumerGroup(request);
    }
}
