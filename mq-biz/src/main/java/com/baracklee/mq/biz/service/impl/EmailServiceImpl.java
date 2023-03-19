package com.baracklee.mq.biz.service.impl;


import com.baracklee.mq.biz.common.util.EmailUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.SendMailRequest;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.EmailService;
import com.baracklee.mq.biz.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
public class EmailServiceImpl implements EmailService {

	@Autowired
	private TopicService topicService;

	@Autowired
	private ConsumerGroupService consumerGroupService;

	@Autowired
	private EmailUtil emailUtil;

	// private final String[] arrInfo = { "info", "warn", "error" };

	@Override
	public void sendConsumerMail(SendMailRequest request) {
		// 使用以前的告警方式
		sendByConsumer(request);
		addLogMsg(request);
	}

	@Override
	public void sendProduceMail(SendMailRequest request) {
		// 使用以前的告警方式
		sendByProducer(request);
		addLogMsg(request);
	}

	private void addLogMsg(SendMailRequest request) {
		
	}

	/**
	 * 原生的根据consumerGroup告警
	 * 
	 * @param request
	 */
	private void sendByConsumer(SendMailRequest request) {
		if (request != null && (!StringUtils.isEmpty(request.getConsumerGroupName())
				|| !StringUtils.isEmpty(request.getTopicName()))) {
			if (request.getType() > 0 && request.getType() < 3) {
				ConsumerGroupEntity consumerGroupEntity = consumerGroupService.getCache().get(request.getConsumerGroupName());
				if (consumerGroupEntity!=null) {
					ConsumerGroupTopicEntity topicEntity = consumerGroupService.getTopic(request.getConsumerGroupName(),
							request.getTopicName());
					String alarms = consumerGroupEntity.getAlarmEmails();
					if (topicEntity != null) {
						alarms += "," + topicEntity.getAlarmEmails() + ",";
					}
					alarms = alarms.replaceAll(",,", ",");
					emailUtil.sendMail(request.getSubject(), request.getContent(), Arrays.asList(alarms.split(",")),
							request.getType());
				} else if (!Util.isEmpty(request.getTopicName())) {
					sendByProducer(request);
				}
			}
		} else if (request != null && request.getType() > 0 && request.getType() < 3) {
			emailUtil.sendMail(request.getSubject(), request.getContent(), null, request.getType());
		}
	}

	/**
	 * 原生的根据producer告警
	 * 
	 * @param request
	 */
	private void sendByProducer(SendMailRequest request) {
		if (request != null && !StringUtils.isEmpty(request.getTopicName())) {
			if (request.getType() > 0 && request.getType() < 3) {
				TopicEntity topicEntity = topicService.getCache().get(request.getTopicName());
				if (topicEntity!=null) {
					String alarms = topicEntity.getEmails() + "";
					emailUtil.sendMail(request.getSubject(), request.getContent(), Arrays.asList(alarms.split(",")),
							request.getType());
				}
			}
		} else if (request != null && request.getType() > 0 && request.getType() < 3) {
			emailUtil.sendMail(request.getSubject(), request.getContent(), null, request.getType());
		}
	}
}
