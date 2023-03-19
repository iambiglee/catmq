package com.baracklee.mq.biz.service;


import com.baracklee.mq.biz.dto.client.SendMailRequest;

public interface EmailService {
	void sendConsumerMail(SendMailRequest request);
	void sendProduceMail(SendMailRequest request);
}
