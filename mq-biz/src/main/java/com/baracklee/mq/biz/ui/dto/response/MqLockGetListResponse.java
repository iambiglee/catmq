package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.MqLockEntity;

import java.util.List;

/**
 * @author Barack Lee
 */
public class MqLockGetListResponse extends BaseUiResponse<List<MqLockEntity>> {
    public MqLockGetListResponse(Long count, List<MqLockEntity> data){
        super(count, data);
    }
}
