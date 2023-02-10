package com.baracklee.mq.client.core.impl.queueutils;

import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.dto.base.MessageDto;
import com.baracklee.mq.biz.event.IAsynSubscriber;
import com.baracklee.mq.biz.event.ISubscriber;

import java.util.ArrayList;
import java.util.List;

public class MessageInvokeCommandForThreadIsolation {


    public static List<Long> invoke(List<MessageDto> dtos, ISubscriber iSubscriber, IAsynSubscriber iAsynSubscriber,
                                    ConsumerQueueDto pre){
        List<Long> failIds= new ArrayList<>();
        if(iSubscriber!=null){
            failIds=iSubscriber.onMessageReceived(dtos);
            return failIds==null?new ArrayList<>():failIds;
        }else if(iAsynSubscriber!=null){
            iAsynSubscriber.onMessageReceived(dtos, JsonUtil.copy(pre,ConsumerQueueDto.class));
        }
        return failIds;
    }
}
