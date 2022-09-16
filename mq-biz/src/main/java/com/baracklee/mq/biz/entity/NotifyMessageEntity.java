package com.baracklee.mq.biz.entity;

import java.util.Date;

public class NotifyMessageEntity {
    /**
     *
     */
    private long id;

    /**
     *
     */
    private long consumerGroupId;

    /**
     * 1,表示触发重平衡，2，表示同步数据
     */
    private int messageType;

    /**
     * 操作人
     */
    private String insertBy;

    /**
     * 创建时间
     */
    private Date insertTime;

    /**
     * 操作人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    private int isActive;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(long consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getInsertBy() {
        return insertBy;
    }

    public void setInsertBy(String insertBy) {
        this.insertBy = insertBy;
    }

    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
}
