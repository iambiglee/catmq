package com.baracklee.rest.mq.spi;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.service.UserInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  BarackLee
 */
@Service
public class DefaultUserInfoHolder implements UserInfoHolder {
    private SoaConfig soaConfig;

    @Autowired
    public DefaultUserInfoHolder(SoaConfig soaConfig) {
        this.soaConfig = soaConfig;
    }


    @Override
    public UserInfo getUser() {
        UserInfo userInfo=new UserInfo();
        userInfo.setAdmin(true);
        userInfo.setUserId(soaConfig.getMqAdminUser());
        userInfo.setName(soaConfig.getMqAdminUser());
        return userInfo;
    }

    @Override
    public String getUserId() {
        return soaConfig.getMqAdminUser();
    }

    @Override
    public void setUserId(String userId) {

    }

    @Override
    public void clear() {

    }
}
