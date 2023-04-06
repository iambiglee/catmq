package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.biz.dto.client.CommitOffsetResponse;

import java.util.Map;

/**
 * Author:  BarackLee
 */
public interface ConsumerCommitService {
    CommitOffsetResponse commitOffset(CommitOffsetRequest request);

    Map<Long, ConsumerQueueVersionDto> getCache();
}
