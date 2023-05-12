package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.DepartmentVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class DepartmentReportResponse extends BaseUiResponse<List<DepartmentVo>> {
    public DepartmentReportResponse(Long count, List<DepartmentVo> data) {
        super(count, data);
    }
}
