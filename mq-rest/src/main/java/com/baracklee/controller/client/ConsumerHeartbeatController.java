package com.baracklee.controller.client;

import com.baracklee.MqConstanst;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.client.HeartbeatRequest;
import com.baracklee.mq.biz.dto.client.HeartbeatResponse;
import com.baracklee.mq.biz.service.ConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping(MqConstanst.CONSUMERPRE)
public class ConsumerHeartbeatController {
    private Logger logger= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<Long, Boolean> mapAppPolling = new ConcurrentHashMap<>(1000);

    private ConsumerService consumerService;
    private SoaConfig soaConfig;

    @Autowired
    public ConsumerHeartbeatController(ConsumerService consumerService, SoaConfig config) {
        this.consumerService = consumerService;
        this.soaConfig = config;
    }

    private volatile int heartBeatThreadSize=3;


    @PostMapping("/heartbeat")
    public HeartbeatResponse heartBeat(@RequestBody HeartbeatRequest request) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setSuc(true);
        response.setHeatbeatTime(soaConfig.getConsumerHeartBeatTime());
        response.setBakUrl(soaConfig.getMqBakUrl());
        try {
            if (request != null) {
                if (request.getAsyn() == 1) {
                    if (request.getConsumerId() > 0) {
                        mapAppPolling.put(request.getConsumerId(), true);
                    }
                    if (!CollectionUtils.isEmpty(request.getConsumerIds())) {
                        request.getConsumerIds().forEach(t1 -> {
                            mapAppPolling.put(t1, true);
                        });
                    }
                }else {
                    response.setDeleted(consumerService.heartbeat(Arrays.asList(request.getConsumerId()))>0?0:1);
                }
            }

        } catch (Throwable e) {
            logger.error("consumerHeartbeat_error",e);
        }
        return response;
    }
}
