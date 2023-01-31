package com.baracklee.mq.client.resource;

import com.baracklee.mq.biz.dto.client.*;

import java.util.List;

public class MqResource implements IMqResource{

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
