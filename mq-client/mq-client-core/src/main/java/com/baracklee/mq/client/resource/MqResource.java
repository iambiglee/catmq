package com.baracklee.mq.client.resource;

import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.IHttpClient;
import com.baracklee.mq.biz.dto.client.*;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MqResource implements IMqResource{


    private final Logger logger = LoggerFactory.getLogger(MqResource.class);
    private IHttpClient httpClient = null;
    private AtomicReference<List<String>> urlsG1 = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<String>> urlsG2 = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<String>> urlsOrigin = new AtomicReference<>(new ArrayList<>());
    // private AtomicInteger couter = new AtomicInteger(0);
    private Map<String, Long> failUrlG1 = new ConcurrentHashMap<>();
    private Map<String, Long> failUrlG2 = new ConcurrentHashMap<>();
    private ThreadPoolExecutor executor = null, executor1 = null;
    private AtomicLong counterG1 = new AtomicLong(0);
    private AtomicLong counterG2 = new AtomicLong(0);

    public MqResource(String url, long connectionTimeOut, long readTimeOut) {
        this(new HttpClient(connectionTimeOut,readTimeOut),url);
    }

    public MqResource(HttpClient httpClient, String url) {
        this
    }


    @Override
    public void setUrls(List<String> urlsTempG1, List<String> urlsTempG2) {

    }

    @Override
    public long register(ConsumerRegisterRequest request) {
        return 0;
    }

    @Override
    public void publishAndUpdateResultFailMsg(FailMsgPublishAndUpdateResultRequest request) {

    }

    @Override
    public void deRegister(ConsumerDeRegisterRequest request) {

    }

    @Override
    public GetMetaGroupResponse getMetaGroup(GetMetaGroupRequest request) {
        return null;
    }

    @Override
    public GetTopicResponse getTopic(GetTopicRequest request) {
        return null;
    }

    @Override
    public GetGroupTopicResponse getGroupTopic(GetGroupTopicRequest request) {
        return null;
    }

    @Override
    public void addCat(CatRequest request) {

    }

    @Override
    public boolean publish(PublishMessageRequest request, int retryTimes) {
        return false;
    }

    @Override
    public boolean publish(PublishMessageRequest request) {
        return false;
    }

    @Override
    public void commitOffset(CommitOffsetRequest request) {

    }

    @Override
    public ConsumerGroupRegisterResponse registerConsumerGroup(ConsumerGroupRegisterRequest request) {
        return null;
    }

    @Override
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {
        return null;
    }

    @Override
    public GetConsumerGroupResponse getConsumerGroup(GetConsumerGroupRequest request) {
        return null;
    }

    @Override
    public GetMessageCountResponse getMessageCount(GetMessageCountRequest request) {
        return null;
    }

    @Override
    public PullDataResponse pullData(PullDataRequest request) {
        return null;
    }

    @Override
    public GetTopicQueueIdsResponse getTopicQueueIds(GetTopicQueueIdsRequest request) {
        return null;
    }

    @Override
    public void addLog(LogRequest request) {

    }

    @Override
    public void addOpLog(OpLogRequest request) {

    }

    @Override
    public void sendMail(SendMailRequest request) {

    }

    @Override
    public String getBrokerIp() {
        return null;
    }
}
