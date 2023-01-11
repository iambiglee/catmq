package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.UserInfo;

public interface UserInfoHolder {
    UserInfo getUser();

    String getUserId();

    void setUserId(String userId);

    void clear();
}
