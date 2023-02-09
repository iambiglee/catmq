package com.baracklee.mq.biz.event;

import com.baracklee.mq.biz.dto.base.MessageDto;

public interface IMsgFilter {
    boolean onMsgFilter(MessageDto dto);
}
