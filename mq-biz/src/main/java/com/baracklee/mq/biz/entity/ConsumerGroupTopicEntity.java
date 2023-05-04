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

    /**
     * 字段名常量值，在构造查询map时，key值就不需要hard code了。
     * 如构造查询ID为121的查询map时，map.put(ConsumerGroupTopicEntity.FdId， "121");
     */

    public final static String TABLE_NAME = "consumer_group_topic";

    public static String FdId = "id";

    public static String FdConsumerGroupId = "consumerGroupId";

    public static String FdConsumerGroupName = "consumerGroupName";

    public static String FdTopicId = "topicId";

    public static String FdTopicName = "topicName";

    public static String FdOriginTopicName = "originTopicName";

    public static String FdTopicType = "topicType";

    public static String FdRetryCount = "retryCount";

    public static String FdThreadSize = "threadSize";

    public static String FdMaxLag = "maxLag";

    public static String FdTag = "tag";

    public static String FdDelayProcessTime = "delayProcessTime";

    public static String FdPullBatchSize = "pullBatchSize";

    public static String FdConsumerBatchSize = "consumerBatchSize";

    public static String FdMaxPullTime = "maxPullTime";

    public static String FdAlarmEmails = "alarmEmails";

    public static String FdInsertBy = "insertBy";

    public static String FdInsertTime = "insertTime";

    public static String FdUpdateBy = "updateBy";

    public static String FdUpdateTime = "updateTime";

    public static String FdIsActive = "isActive";

    public static String FdMetaUpdateTime = "metaUpdateTime";

    public static String FdTimeOut = "timeOut";
}
