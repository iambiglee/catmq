package com.baracklee.mq.biz.dto.base;

import com.baracklee.mq.biz.dto.base.ConsumerGroupMetaDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;

import java.util.Map;

public class ConsumerGroupOneDto {
    private ConsumerGroupMetaDto meta;
    //key为queueid
    private Map<Long, ConsumerQueueDto> queues;

    public ConsumerGroupMetaDto getMeta() {
        return meta;
    }

    public void setMeta(ConsumerGroupMetaDto meta) {
        this.meta = meta;
    }

    public Map<Long, ConsumerQueueDto> getQueues() {
        return queues;
    }

    public void setQueues(Map<Long, ConsumerQueueDto> queues) {
        this.queues = queues;
    }

}
