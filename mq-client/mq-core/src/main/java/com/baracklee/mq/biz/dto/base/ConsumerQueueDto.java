package com.baracklee.mq.biz.dto.base;

public class ConsumerQueueDto extends ConsumerQueueVersionDto{
    private long queueId;

    private String originTopicName;
     private int topicTyle;

     private long topicId;

     private int delayProcessTime = 0;

     private int threadSize=10;

     private int pullBatchSize = 50;

     private int consumerBatchSize = 1;

     private int retryCount =100;

     private int stopFlag= 0;

     private String tag;

     private int traceFlag=0;

     private volatile long lastId=0;

     private int maxPullTime =5;

     private int timeout=0;

    public long getQueueId() {
        return queueId;
    }

    public void setQueueId(long queueId) {
        this.queueId = queueId;
    }

    public String getOriginTopicName() {
        return originTopicName;
    }

    public void setOriginTopicName(String originTopicName) {
        this.originTopicName = originTopicName;
    }

    public int getTopicTyle() {
        return topicTyle;
    }

    public void setTopicTyle(int topicTyle) {
        this.topicTyle = topicTyle;
    }

    public long getTopicId() {
        return topicId;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public int getDelayProcessTime() {
        return delayProcessTime;
    }

    public void setDelayProcessTime(int delayProcessTime) {
        this.delayProcessTime = delayProcessTime;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
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

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(int stopFlag) {
        this.stopFlag = stopFlag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getTraceFlag() {
        return traceFlag;
    }

    public void setTraceFlag(int traceFlag) {
        this.traceFlag = traceFlag;
    }

    public long getLastId() {
        return lastId;
    }

    public void setLastId(long lastId) {
        this.lastId = lastId;
    }

    public int getMaxPullTime() {
        return maxPullTime;
    }

    public void setMaxPullTime(int maxPullTime) {
        this.maxPullTime = maxPullTime;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
