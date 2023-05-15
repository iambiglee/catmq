package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class DbNodeCreateSqlResponse extends BaseUiResponse<List<String>> {
    public DbNodeCreateSqlResponse(Long count, List<String> data) {
        super(count, data);
    }
}
