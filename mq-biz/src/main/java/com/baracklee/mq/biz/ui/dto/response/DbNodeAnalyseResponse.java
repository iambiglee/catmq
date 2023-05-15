package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.AnalyseDto;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

/**
 * @Author： Barack Lee
 */
public class DbNodeAnalyseResponse extends BaseUiResponse<List<AnalyseDto>> {
    public DbNodeAnalyseResponse(Long count, List<AnalyseDto> data) {
        super(count, data);
    }
}
