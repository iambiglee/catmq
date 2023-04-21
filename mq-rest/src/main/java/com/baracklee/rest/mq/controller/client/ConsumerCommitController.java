package com.baracklee.rest.mq.controller.client;


import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.client.CommitOffsetRequest;
import com.baracklee.mq.biz.dto.client.CommitOffsetResponse;
import com.baracklee.mq.biz.service.ConsumerCommitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(MqConstanst.CONSUMERPRE)
public class ConsumerCommitController {
	@Autowired
	private ConsumerCommitService consumerCommitService;

	// 发送心跳，直接返回
	@PostMapping("/commitOffset")
	public CommitOffsetResponse commitOffset(@RequestBody CommitOffsetRequest request) {
		return consumerCommitService.commitOffset(request);
	}

	@GetMapping("/getCommitOffsetCache")
	public Object getCommitOffsetCache() {
		return consumerCommitService.getCache();
	}

}
