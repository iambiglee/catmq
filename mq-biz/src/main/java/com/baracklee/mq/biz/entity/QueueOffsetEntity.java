package com.baracklee.mq.biz.entity;

import java.util.Date;

public class QueueOffsetEntity {
    /**
     *
     */
    private long id;

    /**
     * 订阅者组id
     */
    private long consumerGroupId;

    /**
     * 订阅者组主键
     */
    private String consumerGroupName;

    /**
     * 客户端消费者name
     */
    private String consumerName;

    /**
     *
     */
    private long consumerId;

    /**
     * 主题id
     */
    private long topicId;

    /**
     * 主题名称,如果
     */
    private String topicName;

    /**
     * 如果是失败队列此字段名称表示原始的topic名称，topic_name为consumer_group_name+原始的topic_name+"_fail"，否则topic_name和origin_topic_name一致
     */
    private String originTopicName;

    /**
     * 1,表示正常队列，2，表示失败队列
     */
    private int topicType;

    /**
     * 分区id
     */
    private long queueId;

    /**
     * 消费者提交的偏移量
     */
    private long offset;

    /**
     * 订阅时的起始偏移量
     */
    private long startOffset;

    /**
     * 偏移版本号，当手动修改偏移时，会升级版本号，如果客户端提交更新便宜的时候，只能按照版本号相同，偏移量大的值更新
     */
    private long offsetVersion;

    /**
     * 1,表示客户端此queue停止消费，0，表示正常消费
     */
    private int stopFlag;

    /**
     * ip+db_name +tb_name
     */
    private String dbInfo;

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
     * 原始的消费者组名
     */
    private String originConsumerGroupName;

    /**
     * 1，为集群模式，2，为广播模式,3，为代理模式
     */
    private int consumerGroupMode;

    /**
     *
     */
    private String subEnv;

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

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public long getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(long consumerId) {
        this.consumerId = consumerId;
    }

    public long getTopicId() {
        return topicId;
    }

    public void setTopicId(long topicId) {
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

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public long getOffsetVersion() {
        return offsetVersion;
    }

    public void setOffsetVersion(long offsetVersion) {
        this.offsetVersion = offsetVersion;
    }

    public int getStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(int stopFlag) {
        this.stopFlag = stopFlag;
    }

    public String getDbInfo() {
        return dbInfo;
    }

    public void setDbInfo(String dbInfo) {
        this.dbInfo = dbInfo;
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

    public String getOriginConsumerGroupName() {
        return originConsumerGroupName;
    }

    public void setOriginConsumerGroupName(String originConsumerGroupName) {
        this.originConsumerGroupName = originConsumerGroupName;
    }

    public int getConsumerGroupMode() {
        return consumerGroupMode;
    }

    public void setConsumerGroupMode(int consumerGroupMode) {
        this.consumerGroupMode = consumerGroupMode;
    }

    public String getSubEnv() {
        return subEnv;
    }

    public void setSubEnv(String subEnv) {
        this.subEnv = subEnv;
    }
}
