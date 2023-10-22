package com.baracklee.mq.biz.dto.base;

import java.util.Map;

public class ProducerDataDto {

    private long id;

    private String tag;

    private String bizId;

    private Map<String,String> head;

    private String body;

    private String traceId;

    private int retryCount;

    private PartitionInfo partitionInfo;

    public ProducerDataDto() {

    }

    public ProducerDataDto(String bizId, String body) {
        this(bizId, "", null, body);
    }

    public ProducerDataDto(String body) {
        this("", "", null, body);
    }

    public ProducerDataDto(String bizId, String body, String tag) {
        this(bizId, tag, null, body);
    }

    public ProducerDataDto(String bizId, String tag, Map<String, String> header, String body) {
        setBizId(bizId);
        setTag(tag);
        setHead(header);
        setBody(body);
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public Map<String, String> getHead() {
        return head;
    }

    public void setHead(Map<String, String> head) {
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

    public PartitionInfo getPartitionInfo() {
        return partitionInfo;
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }
}
