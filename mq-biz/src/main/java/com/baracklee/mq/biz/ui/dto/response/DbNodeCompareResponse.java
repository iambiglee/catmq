package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class DbNodeCompareResponse extends BaseUiResponse<List<Long>> {

    public DbNodeCompareResponse(Long count, List<Long> data) {
        super(count, data);
    }
}
