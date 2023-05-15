package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.DbNodeEntity;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class DbNodeGetListResponse extends BaseUiResponse<List<DbNodeEntity>> {

    public DbNodeGetListResponse(Long count, List<DbNodeEntity> data) {
        super(count, data);
    }
}
