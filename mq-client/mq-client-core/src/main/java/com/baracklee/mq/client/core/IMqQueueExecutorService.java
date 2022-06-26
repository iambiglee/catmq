package com.baracklee.mq.client.core;

import java.util.List;
import java.util.Map;

public interface IMqQueueExecutorService extends IMqClientService{
    void updateQueueMeta(ConsumerQueueDto consumerQueue);

    void notifyMsg();

    void commit(List<MessageDto> failMsgs, ConsumerQueueDto consumerQueue);

    Map<Long, TraceMessageDto> getSlowMsg();

    ConsumerQueueVersionDto getChangedCommit();

    ConsumerQueueVersionDto getLast();
    boolean hasFininshed();
    void stop();
}
