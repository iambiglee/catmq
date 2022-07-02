package com.baracklee.mq.biz.dto.client;

import com.baracklee.mq.biz.dto.BaseRequest;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;

import java.util.List;

public class PublishMessageRequest extends BaseRequest {
    private List<ProducerDataDto> msgs;

    private String topicName;

    private String token;

    private int synFalg=1;

    public List<ProducerDataDto> getMsgs() {
        return msgs;
    }

    public void setMsgs(List<ProducerDataDto> msgs) {
        this.msgs = msgs;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getSynFalg() {
        return synFalg;
    }

    public void setSynFalg(int synFalg) {
        this.synFalg = synFalg;
    }
}

