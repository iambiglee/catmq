package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

public class UserGetBizTypesResponse extends BaseUiResponse<List<String>> {
    public UserGetBizTypesResponse(Long count,List<String> data) {
        super(count,data);
    }
}
