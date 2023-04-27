package com.baracklee.mq.biz.ui.dto.response;



import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.ServerEntity;

import java.util.List;

/**
 * @Author：Barack lee
 */
public class ServerGetListResponse extends BaseUiResponse<List<ServerEntity>> {
    public ServerGetListResponse(Long count, List<ServerEntity> data) {
        super(count, data);
    }
}
