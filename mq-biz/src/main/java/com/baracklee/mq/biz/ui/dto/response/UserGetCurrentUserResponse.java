package com.baracklee.mq.biz.ui.dto.response;

import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;

public class UserGetCurrentUserResponse extends BaseUiResponse<UserInfo> {
    public UserGetCurrentUserResponse(UserInfo userInfo) {
        super(userInfo);
    }
}
