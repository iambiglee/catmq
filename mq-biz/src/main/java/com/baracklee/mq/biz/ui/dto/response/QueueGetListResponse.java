package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.QueueVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class QueueGetListResponse extends BaseUiResponse<List<QueueVo>> {

    private int role = 0;

    public QueueGetListResponse(Long count, List<QueueVo> data) {
        super(count, data);
    }

    public QueueGetListResponse(Long count, List<QueueVo> data, int role) {
        super(count, data);
        this.role = role;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }
}
