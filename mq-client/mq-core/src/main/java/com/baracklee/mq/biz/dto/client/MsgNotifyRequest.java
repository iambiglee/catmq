package com.baracklee.mq.biz.dto.client;

import java.util.List;

public class MsgNotifyRequest {
    private List<MsgNotifyDto> msgNotifyDtos;

    public List<MsgNotifyDto> getMsgNotifyDtos() {
        return msgNotifyDtos;
    }

    public void setMsgNotifyDtos(List<MsgNotifyDto> msgNotifyDtos) {
        this.msgNotifyDtos = msgNotifyDtos;
    }
}
