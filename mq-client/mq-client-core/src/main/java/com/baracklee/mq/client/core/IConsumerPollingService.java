package com.baracklee.mq.client.core;

import java.util.Map;

public interface IConsumerPollingService extends IMqClientService{
    Map<String, IMMqGroupExecutorService> getMqExecutors();
}
