package com.baracklee.mq.client.factory;

import com.baracklee.mq.client.core.IConsumerPollingService;

public interface IMqFactory {
    IConsumerPollingService createConsumerPollingService();

}
