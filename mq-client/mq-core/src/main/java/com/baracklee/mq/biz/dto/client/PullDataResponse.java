package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseResponse;
import com.baracklee.mq.biz.dto.base.MessageDto;

import java.util.List;

public class PullDataResponse extends BaseResponse {
    private List<MessageDto> msgs;

    public List<MessageDto> getMsgs() {
        return msgs;
    }

    public void setMsgs(List<MessageDto> msgs) {
        this.msgs = msgs;
    }
}
