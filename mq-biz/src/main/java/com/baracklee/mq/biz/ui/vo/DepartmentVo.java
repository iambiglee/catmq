package com.baracklee.mq.biz.ui.vo;

/**
 * @author Barack Lee
 */
public class DepartmentVo {
    private String name;
    private long publishNum;//每周消息发送量
    private long consumerNum;//每周消息消费量

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPublishNum() {
        return publishNum;
    }

    public void setPublishNum(long publishNum) {
        this.publishNum = publishNum;
    }

    public long getConsumerNum() {
        return consumerNum;
    }

    public void setConsumerNum(long consumerNum) {
        this.consumerNum = consumerNum;
    }
}
