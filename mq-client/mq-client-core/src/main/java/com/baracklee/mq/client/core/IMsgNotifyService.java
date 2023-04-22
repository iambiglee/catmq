package com.baracklee.mq.client.core;

import com.baracklee.mq.biz.dto.client.MsgNotifyRequest;

public interface IMsgNotifyService {

    void notify(MsgNotifyRequest request);
}
