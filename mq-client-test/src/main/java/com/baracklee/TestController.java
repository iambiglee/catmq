package com.baracklee;

import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ProducerDataDto;
import com.baracklee.mq.client.MqClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executors;

;
@RestController
public class TestController {
	@GetMapping("/test1")
	public void test1(@RequestParam String topicName, @RequestParam int count) {
		if(Util.isEmpty(topicName))return;
		Executors.newSingleThreadExecutor().submit(new Runnable() {		
			@Override
			public void run() {
				for(int i=1;i<count;i++) {
					try {
						MqClient.publish(topicName, "",new ProducerDataDto(String.valueOf(i)));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Util.sleep(10);
				}
			}
		});
	}
	
}
