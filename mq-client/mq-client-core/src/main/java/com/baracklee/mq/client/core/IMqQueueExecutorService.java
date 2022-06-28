package com.baracklee.mq.client.core;

import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.client.dto.TraceMessageDto;

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
