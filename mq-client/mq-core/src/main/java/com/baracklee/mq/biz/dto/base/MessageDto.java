package com.baracklee.mq.biz.dto.base;

import java.util.Date;

public class MessageDto extends ProducerDataDto{

    private long id;

    private String topicName;

    private String consumerGroupName;

    private String sendIP;

    private int retryCount;

    private Date sendTime;

    public MessageDto() {
    }

    public MessageDto(long id, String topicName, String consumerGroupName, String sendIP, int retryCount, Date sendTime) {
        this.id = id;
        this.topicName = topicName;
        this.consumerGroupName = consumerGroupName;
        this.sendIP = sendIP;
        this.retryCount = retryCount;
        this.sendTime = sendTime;
    }
}
