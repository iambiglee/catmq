package com.baracklee.mq.biz.entity;

import java.util.Date;

public class ConsumerGroupTopicEntity {
    private long id;
    private long consumerGroupId;
    private String consumerGroupName;
    private Long topicId;
    private String topicName;
    private String originTopicName;
    //1表示正常，2表示失败
    private int topicType;
    private int retryCount;
    private int threadSize;
    private int maxLag;
    private String tag;
    private int delayProcessTime;
    private int pullBatchSize;
    private int consumerBatchSize;
    private int maxPullTime;
    private String alarmEmails;
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

    /**
     * 元数据更新时间
     */
    private Date metaUpdateTime;

    /**
     * 客户端消费超时熔断时间单位秒,0表示不熔断
     */
    private int timeOut;

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

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getOriginTopicName() {
        return originTopicName;
    }

    public void setOriginTopicName(String originTopicName) {
        this.originTopicName = originTopicName;
    }

    public int getTopicType() {
        return topicType;
    }

    public void setTopicType(int topicType) {
        this.topicType = topicType;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getThreadSize() {
        return threadSize;
    }

    public void setThreadSize(Integer threadSize) {
        this.threadSize = threadSize;
    }

    public Integer getMaxLag() {
        return maxLag;
    }

    public void setMaxLag(int maxLag) {
        this.maxLag = maxLag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getDelayProcessTime() {
        return delayProcessTime;
    }

    public void setDelayProcessTime(int delayProcessTime) {
        this.delayProcessTime = delayProcessTime;
    }

    public int getPullBatchSize() {
        return pullBatchSize;
    }

    public void setPullBatchSize(int pullBatchSize) {
        this.pullBatchSize = pullBatchSize;
    }

    public int getConsumerBatchSize() {
        return consumerBatchSize;
    }

    public void setConsumerBatchSize(int consumerBatchSize) {
        this.consumerBatchSize = consumerBatchSize;
    }

    public int getMaxPullTime() {
        return maxPullTime;
    }

    public void setMaxPullTime(int maxPullTime) {
        this.maxPullTime = maxPullTime;
    }

    public String getAlarmEmails() {
        return alarmEmails;
    }

    public void setAlarmEmails(String alarmEmails) {
        this.alarmEmails = alarmEmails;
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

    public Date getMetaUpdateTime() {
        return metaUpdateTime;
    }

    public void setMetaUpdateTime(Date metaUpdateTime) {
        this.metaUpdateTime = metaUpdateTime;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }
}
