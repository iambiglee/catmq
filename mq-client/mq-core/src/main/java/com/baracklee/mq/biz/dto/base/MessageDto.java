package com.baracklee.mq.biz.dto.base;

import java.util.Date;
import java.util.Map;

public class MessageDto extends ProducerDataDto {

    private long id;

    private String topicName;

    private String consumerGroupName;

    private String sendIP;

    private int retryCount;

    private Date sendTime;

    public MessageDto() {
    }

    public MessageDto(String bizId, String tag, Map<String, String> header, String body) {
        super.setBizId(bizId);
        super.setTag(tag);
        super.setHead(header);
        super.setBody(body);
    }
    public MessageDto(String body){this("","",null,body);}

    public MessageDto(String bizId,String body){this(bizId,"",null,body);}

    public MessageDto(String bizId, String body,String tag){this(bizId,tag,null,body);}

    public MessageDto(String bizId, String tag, Map<String, String> header, String body, String sendIp) {
        super.setBizId(bizId);
        super.setTag(tag);
        super.setHead(header);
        super.setBody(body);
        setSendIP(sendIp);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }

    public String getSendIP() {
        return sendIP;
    }

    public void setSendIP(String sendIP) {
        this.sendIP = sendIP;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
}
