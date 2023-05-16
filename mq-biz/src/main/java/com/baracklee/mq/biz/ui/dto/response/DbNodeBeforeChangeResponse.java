package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

/**
 * @author Barack Lee
 */
public class DbNodeBeforeChangeResponse extends BaseUiResponse<Void> {
    public DbNodeBeforeChangeResponse() {
        super();
    }

    public DbNodeBeforeChangeResponse(String code, String msg) {
        super(code, msg);
    }
}
