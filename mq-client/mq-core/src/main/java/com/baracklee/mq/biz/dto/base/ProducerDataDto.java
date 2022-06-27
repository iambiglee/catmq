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
}
