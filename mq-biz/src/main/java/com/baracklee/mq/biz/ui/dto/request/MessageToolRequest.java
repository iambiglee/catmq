package com.baracklee.mq.biz.ui.dto.request;

import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.dto.request.BaseUiRequst;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;

/**
 * @author Barack Lee
 */
public class MessageToolRequest extends BaseUiRequst {
    private String topicName;
    private int sendType;

    private String mqSubEnv;

    private MessageDto message;

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getSendType() {
        return sendType;
    }

    public void setSendType(int sendType) {
        this.sendType = sendType;
    }

    public MessageDto getMessage() {
        return message;
    }

    public void setMessage(MessageDto message) {
        this.message = message;
    }

    public String getMqSubEnv() {
        return mqSubEnv;
    }

    public void setMqSubEnv(String mqSubEnv) {
        this.mqSubEnv = mqSubEnv;
    }
}
