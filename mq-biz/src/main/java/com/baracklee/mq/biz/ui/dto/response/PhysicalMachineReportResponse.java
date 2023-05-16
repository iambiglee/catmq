package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.vo.PhysicalMachineReportVo;

import java.util.List;

/**
 * @author Barack Lee
 */
public class PhysicalMachineReportResponse extends BaseUiResponse<List<PhysicalMachineReportVo>> {
    public PhysicalMachineReportResponse(Long count, List<PhysicalMachineReportVo> data) {
        super(count, data);
    }
}