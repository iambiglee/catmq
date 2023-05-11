package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

/**
 * @Author： Barack Lee
 */
public class QueueOffsetUpdateResponse extends BaseUiResponse<Void> {
    public QueueOffsetUpdateResponse() {
        super();
    }

    public QueueOffsetUpdateResponse(String code, String msg) {
        super(code, msg);
    }
}
