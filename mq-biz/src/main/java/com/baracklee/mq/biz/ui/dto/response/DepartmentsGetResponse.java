package com.baracklee.mq.biz.ui.dto.response;


import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

public class DepartmentsGetResponse extends BaseUiResponse<List<String>> {
    public DepartmentsGetResponse(Long count, List<String> data) {
        super(count, data);
    }
}
