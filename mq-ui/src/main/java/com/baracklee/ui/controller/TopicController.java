package com.baracklee.ui.controller;


import com.baracklee.mq.biz.dto.request.TopicCreateRequest;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.ui.dto.request.TopicGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.*;
import com.baracklee.ui.service.UiTopicService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/topic")
public class TopicController {
    @Autowired
    UiTopicService uiTopicService;

    private Logger log = LoggerFactory.getLogger(TopicController.class);

    @RequestMapping("/list/data")
    public TopicGetListResponse topicListData(TopicGetListRequest topicGetListRequest) {
        return uiTopicService.queryByPage(topicGetListRequest);
    }

    @PostMapping("/createOrUpdate")
    public TopicCreateResponse createOrUpdateTopic(TopicCreateRequest topicCreateRequest) {
        return uiTopicService.createOrUpdateTopic(topicCreateRequest);
    }

    @PostMapping("/delete")
    public TopicDeleteResponse deleteTopic(@RequestParam("id") Long topicId) {
        return uiTopicService.deleteTopic(topicId);
    }

    @PostMapping("/expand")
    public TopicExpandResponse expandTopic(@RequestParam("id") Long topicId) {
        return uiTopicService.expandTopic(topicId);
    }


    @PostMapping("/generateToken")
    public TopicGenerateTokenResponse generateToken(@RequestParam("id") Long topicId) {
        return uiTopicService.generateToken(topicId);
    }

    @PostMapping("/clearToken")
    public TopicClearTokenResponse clearToken(@RequestParam("id") Long topicId) {
        return uiTopicService.clearToken(topicId);
    }

    @PostMapping("/searchTopics")
    public TopicSearchResponse searchTopics(String keyword, int offset, int limit, String consumerGroupName) {
        if (StringUtils.isEmpty(keyword)) {
            return new TopicSearchResponse();
        } else {
            List<TopicEntity> topicEntityList = uiTopicService.getSelectSearch(keyword, offset, limit, consumerGroupName);
            return new TopicSearchResponse((long) topicEntityList.size(), topicEntityList);
        }

    }

    @GetMapping("/removeQueue/list")
    public TopicQueueRemoveListResponse queueRemoveList(Long topicId) {
        return uiTopicService.queueRemoveList(topicId);
    }

    @GetMapping("/getById")
    public TopicGetByIdResponse getById(Long id) {
        return uiTopicService.getById(id);
    }

    @PostMapping("/updateSaveDayNum")
    public TopicUpdateSaveDayNumResponse updateSaveDayNum(Long topicId, int num) {
        uiTopicService.updateSaveDayNum(topicId, num);
        return new TopicUpdateSaveDayNumResponse();
    }

    @PostMapping("/queue/remove")
    public TopicQueueRemoveResponse remove(@RequestParam("queueId") Long queueId, @RequestParam("topicId") Long topicId) {
        return uiTopicService.queueRemove(queueId, topicId);
    }

    @PostMapping("/manualExpand")
    public TopicManualExpandResponse manualExpand(@RequestParam("id") Long id, @RequestParam("queueId") Long queueId) {
        return uiTopicService.manualExpand(id, queueId);
    }

    @RequestMapping("/getTopicNames")
    public TopicGetTopicNamesResponse getTopicNames(String keyword, int offset, int limit){
        if (StringUtils.isEmpty(keyword)) {
            return new TopicGetTopicNamesResponse(0L, null);
        }
        return uiTopicService.getTopicNames(keyword, offset,limit);
    }

    @RequestMapping("/getTopicNamesForMessageTool")
    public TopicGetTopicNamesResponse getTopicNamesForMessageTool(String keyword, int offset, int limit){
        if (StringUtils.isEmpty(keyword)) {
            return new TopicGetTopicNamesResponse(0L, null);
        }
        return uiTopicService.getTopicNamesForMessageTool(keyword, offset,limit);
    }

    @RequestMapping("/report/data")
    public TopicReportResponse getTopicReport(TopicGetListRequest topicGetListRequest){
        return uiTopicService.getTopicReport(topicGetListRequest);
    }

    @RequestMapping("/msgCount")
    public BaseUiResponse<String> getTopicMsgCount(@RequestParam("topicName") String topicName, @RequestParam("startTime") String startTime, @RequestParam("endTime") String endTime){
        return uiTopicService.getTopicMsgCount(topicName,startTime,endTime);
    }

}