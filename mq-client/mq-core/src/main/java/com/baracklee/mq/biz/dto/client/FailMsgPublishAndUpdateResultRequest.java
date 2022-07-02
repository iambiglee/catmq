package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;

import java.util.List;

public class FailMsgPublishAndUpdateResultRequest extends BaseRequest {
    private List<Long> ids;

    private long queueId;

    private PublishMessageRequest failMsg;

}
