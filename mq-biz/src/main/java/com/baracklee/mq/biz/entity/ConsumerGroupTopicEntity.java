package com.baracklee.mq.biz.entity;

import java.util.Date;

public class ConsumerGroupTopicEntity {
    private long id;
    private long consumerGroupId;
    private String consumerGroupName;
    private String topicId;
    private String topicName;
    private String originTopicName;
    //1表示正常，2表示失败
    private String topicType;
    private String retryCount;
    private String threadSize;
    private String maxLag;
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


}
