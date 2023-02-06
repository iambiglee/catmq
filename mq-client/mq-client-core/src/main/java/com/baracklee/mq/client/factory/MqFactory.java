package com.baracklee.mq.client.factory;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.client.core.*;
import com.baracklee.mq.client.core.impl.MqBrokerUrlRefreshService;
import com.baracklee.mq.client.core.impl.MqCheckService;
import com.baracklee.mq.client.core.impl.MqGroupExecutorService;
import com.baracklee.mq.client.core.impl.MqMeticsReporterService;
import com.baracklee.mq.client.resource.IMqResource;

public class MqFactory implements IMqFactory{

    private static final Object lockObj = new Object();

    private MqBrokerUrlRefreshService mqBrokerUrlRefreshService;

    @Override
    public IMqBrokerUrlRefreshService createMqBrokerUrlRefreshService() {
        if (mqBrokerUrlRefreshService == null) {
            synchronized (lockObj) {
                if (mqBrokerUrlRefreshService == null) {
                    mqBrokerUrlRefreshService = new MqBrokerUrlRefreshService();
                }
            }
        }
        return mqBrokerUrlRefreshService;
    }

    //检查local和broker 的数据是否同步的线程
    private MqCheckService mqCheckService;

    @Override
    public IMqCheckService createMqCheckService() {
        if(mqCheckService==null)
            synchronized (lockObj){
            if(mqCheckService==null){
                mqCheckService=new MqCheckService();
            }
            }
        return mqCheckService;
    }

    @Override
    public IMqGroupExecutorService createMqGroupExecutorService() {
        return new MqGroupExecutorService();
    }

    private MqHeartbeatService mqHeartbeatService;

    @Override
    public IMqHeartbeatService createMqHeartbeatService() {
        return null;
    }

    @Override
    public IMqMeticsReporterService createMqMeticReporterService() {
        return MqMeticsReporterService.getInstance();
    }

    @Override
    public IMqQueueExecutorService createMqQueueExcutorService(String consumerGroupName, ConsumerQueueDto consumerQueue) {
        return null;
    }

    @Override
    public IMqTopicQueueRefreshService createMqTopicQueueRefreshService() {
        return null;
    }

    @Override
    public IConsumerPollingService createConsumerPollingService() {
        return null;
    }

    @Override
    public IMqResource createMqResource(String url, long connectionTimeOut, long readTimeOut) {
        return null;
    }

    @Override
    public IMsgNotifyService createMsgNotifyService() {
        return null;
    }

    @Override
    public IMqCommitService createCommitService() {
        return null;
    }


}
