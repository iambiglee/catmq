package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.PanelNodeVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class PanelNodeGetListResponse extends BaseUiResponse<List<PanelNodeVo>> {
    public PanelNodeGetListResponse(Long count, List<PanelNodeVo> data) {
        super(count, data);
    }

}
