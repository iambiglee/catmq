package com.baracklee.mq.client.factory;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.client.core.*;
import com.baracklee.mq.client.resource.IMqResource;

public interface IMqFactory {
    IMqBrokerUrlRefreshService createMqBrokerUrlRefreshService();

    IMqCheckService createMqCheckService();

    IMqGroupExecutorService createMqGroupExecutorService();

    IMqHeartbeatService createMqHeartbeatService();

    IMqMeticsReporterService createMqMeticReporterService();

    IMqQueueExecutorService createMqQueueExcutorService(String consumerGroupName,
                                                       ConsumerQueueDto consumerQueue);

    IMqTopicQueueRefreshService createMqTopicQueueRefreshService();

    IConsumerPollingService createConsumerPollingService();

    IMqResource createMqResource(String url, long connectionTimeOut, long readTimeOut);
    IMsgNotifyService createMsgNotifyService();
    IMqCommitService createCommitService();



}
