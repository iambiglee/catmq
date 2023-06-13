package com.baracklee.rest.mq.controller.client;

import com.baracklee.mq.biz.cache.ConsumerGroupCacheService;
import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.ConsumerGroupChangedListener;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.base.ConsumerGroupDto;
import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupRequest;
import com.baracklee.mq.biz.dto.client.GetConsumerGroupResponse;
import com.baracklee.mq.biz.service.ConsumerService;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author:  BarackLee
 */
@RestController
@RequestMapping(MqConstanst.CONSUMERPRE)
public class ConsumerGroupNotifyController implements ConsumerGroupChangedListener {
    private ConsumerGroupCacheService consumerGroupCacheService;

    private ConsumerService consumerService;

    private SoaConfig soaConfig;

    private final Map<GetConsumerGroupRequest, DeferredResult<GetConsumerGroupResponse>> mapAppPolling = new ConcurrentHashMap<>();
    private static AtomicLong longPollingCounter = new AtomicLong(0);

    @Autowired
    public ConsumerGroupNotifyController(ConsumerGroupCacheService consumerGroupCacheService, ConsumerService consumerService, SoaConfig soaConfig) {
        this.consumerGroupCacheService = consumerGroupCacheService;
        this.consumerService = consumerService;
        this.soaConfig = soaConfig;
    }
    @GetMapping("/getConsumerGroupPollingCount")
    public long getServicePollingCount(){
        return longPollingCounter.get();
    }

    @PostMapping("/getConsumerGroupPolling")
    public DeferredResult<GetConsumerGroupResponse> getConsumerGroupPolling(
            @RequestBody GetConsumerGroupRequest request){
        GetConsumerGroupResponse response = new GetConsumerGroupResponse();
        response.setSuc(true);
        response.setSleepTime(RandomUtils.nextInt(50,2000));
        response.setBrokerMetaMode(soaConfig.getBrokerMetaMode());
        DeferredResult<GetConsumerGroupResponse> deferredResult = new DeferredResult<>(
                soaConfig.getPollingTimeOut() * 1000L, response);
        GetConsumerGroupResponse getApplicationResponse = doCheckConsumerGroupPolling(request);
        if(getApplicationResponse!=null){
            deferredResult.setResult(getApplicationResponse);
        }else {
            long count = longPollingCounter.incrementAndGet();
            if(count>soaConfig.getPollingSize()){
                response.setSleepTime(RandomUtils.nextInt(50,200));
                deferredResult.setResult(response);
                longPollingCounter.decrementAndGet();
            }else {
                mapAppPolling.put(request,deferredResult);
                deferredResult.onCompletion(()->{
                    if (mapAppPolling.remove(request)!=null){
                        long count1 = longPollingCounter.decrementAndGet();
                    }
                });
            }
        }
        return deferredResult;
    }


    @Override
    public void onChange() {
        notifyMessage();
    }

    private void notifyMessage() {
        int notifyBatchSize=0;
        for (GetConsumerGroupRequest request : mapAppPolling.keySet()) {
            notifyBatchSize++;
            GetConsumerGroupResponse response = doCheckConsumerGroupPolling(request);
            if (response != null && mapAppPolling.containsKey(request)) {
                mapAppPolling.get(request).setResult(response);
            }
            if (soaConfig.getNotifyWaitTime() > 0 && notifyBatchSize > soaConfig.getNotifyBatchSize()) {
                Util.sleep(soaConfig.getNotifyWaitTime());
            }
        }

    }

    private GetConsumerGroupResponse doCheckConsumerGroupPolling(GetConsumerGroupRequest request) {
        Map<String, ConsumerGroupDto> consumerGroupMap = consumerGroupCacheService.getCache();
        GetConsumerGroupResponse response = new GetConsumerGroupResponse();
        response.setSuc(true);
        response.setConsumerDeleted(0);
        response.setBrokerMetaMode(soaConfig.getBrokerMetaMode());
        try {
            if (consumerService.get(request.getConsumerId()) == null) {
                response.setConsumerDeleted(1);
                return response;
            }
        } catch (Exception e) {
            //给客户端重新连接的时间
            Util.sleep(1000L);
        }

        Map<String, ConsumerGroupOneDto> dataRs = new HashMap<>();

        //key: consumerGroupName  value: consumerGroupName的版本号
        for (Map.Entry<String, Long> t1 : request.getConsumerGroupVersion().entrySet()) {
            if(consumerGroupMap.containsKey(t1.getKey())
            &&t1.getValue()<consumerGroupMap.get(t1.getKey()).getMeta().getVersion()){
                ConsumerGroupOneDto consumerGroupOneDto = new ConsumerGroupOneDto();
                consumerGroupOneDto.setMeta(consumerGroupMap.get(t1.getKey()).getMeta());
                consumerGroupOneDto.setQueues(new HashMap<>());
                if (consumerGroupMap.get(t1.getKey()).getConsumers() != null
                        && consumerGroupMap.get(t1.getKey()).getConsumers().containsKey(request.getConsumerId())) {
                    consumerGroupOneDto
                            .setQueues(consumerGroupMap.get(t1.getKey()).getConsumers().get(request.getConsumerId()));
                }
                dataRs.put(t1.getKey(),consumerGroupOneDto);
            }
            
        }
        if (dataRs.size()>0){
            response.setConsumerGroups(dataRs);
            return response;
        }else {
            return null;
        }
    }
}
