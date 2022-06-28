package com.baracklee.mq.client.dto;

import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public class TraceMessageDto {

    private String inTime;

    private String startTime;

    @JsonIgnore
    public transient long start;
    private long id;
    @JsonIgnore
    private transient MessageDto message;

    private String topic;

    private String group;

    private long queueId;

    public String getInTime() {
        return inTime;
    }

    public void setInTime(String inTime) {
        this.inTime = inTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MessageDto getMessage() {
        return message;
    }

    public void setMessage(MessageDto message) {
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    void start(){
        start=System.currentTimeMillis();
        startTime= Util.formateDate(new Date());
    }
}
