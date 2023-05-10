package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.ConnectionsVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class DbNodeConnectionsResponse extends BaseUiResponse<List<ConnectionsVo>> {
    public DbNodeConnectionsResponse(String code, String msg) {
        super(code, msg);
    }

    public DbNodeConnectionsResponse(Long count, List<ConnectionsVo> data) {
        super(count, data);
    }
}
