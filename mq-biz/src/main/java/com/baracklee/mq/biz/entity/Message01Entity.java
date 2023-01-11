package com.baracklee.mq.biz.entity;

import java.util.Date;

public class Message01Entity {
    /**
     * 主键
     */
    private long id;

    /**
     * 业务id
     */
    private String bizId;

    /**
     * 标记
     */
    private String tag;

    /**
     * 消息头
     */
    private String head;

    /**
     * 消息体
     */
    private String body;

    /**
     * 发送的ip
     */
    private String traceId;

    /**
     * 失败重试次数
     */
    private int retryCount;

    /**
     * 发送的ip
     */
    private String sendIp;

    /**
     * 创建时间
     */
    private Date sendTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getSendIp() {
        return sendIp;
    }

    public void setSendIp(String sendIp) {
        this.sendIp = sendIp;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
}
