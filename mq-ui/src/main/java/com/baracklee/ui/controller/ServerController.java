package com.baracklee.ui.controller;


import com.alibaba.fastjson2.JSONArray;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.ServerEntity;
import com.baracklee.mq.biz.ui.dto.request.ServerGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ServerGetListResponse;
import com.baracklee.ui.service.UiServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/server")
public class ServerController {
	@Autowired
	private UiServerService uiServerService;
	Logger log = LoggerFactory.getLogger(ServerController.class);

	@RequestMapping("/list/data")
	public ServerGetListResponse findBy(ServerGetListRequest serverGetListRequest) {
		return uiServerService.findBy(serverGetListRequest);
	}

	@RequestMapping("/changeStatusFlag")
	public BaseUiResponse changeStatusFlag(String serverId) {
		return uiServerService.changeStatusFlag(serverId);
	}

	@RequestMapping("/batchPull")
	@ResponseBody
	public BaseUiResponse batchPull(@RequestParam("servers") String servers) {
		List<ServerEntity> serverList = JSONArray.parseArray(servers, ServerEntity.class);

		return uiServerService.batchPull(serverList);
	}

	@RequestMapping("/batchPush")
	@ResponseBody
	public BaseUiResponse batchPush(@RequestParam("servers") String servers) {
		List<ServerEntity> serverList = JSONArray.parseArray(servers, ServerEntity.class);
		return uiServerService.batchPush(serverList);
	}

	@RequestMapping("/onLineServer")
	public BaseUiResponse<String> onLineServer(){
		return uiServerService.onLineServer();

	}

}
