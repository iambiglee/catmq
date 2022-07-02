package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;
import com.baracklee.mq.biz.dto.base.ConsumerQueueVersionDto;

import java.util.List;

public class CommitOffsetRequest extends BaseRequest {
    private List<ConsumerQueueVersionDto> queueOffsets;

    private int flag=0;

    public List<ConsumerQueueVersionDto> getQueueOffsets() {
        return queueOffsets;
    }

    public void setQueueOffsets(List<ConsumerQueueVersionDto> queueOffsets) {
        this.queueOffsets = queueOffsets;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
