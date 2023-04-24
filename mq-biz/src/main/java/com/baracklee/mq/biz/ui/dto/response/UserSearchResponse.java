package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;

import java.util.List;

public class UserSearchResponse extends BaseUiResponse<List<UserInfo>> {
    public UserSearchResponse(Long count, List<UserInfo> data) {
        super(count, data);
    }
}
