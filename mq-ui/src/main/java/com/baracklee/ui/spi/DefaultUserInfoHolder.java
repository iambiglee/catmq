package com.baracklee.ui.spi;

import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.service.UserInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultUserInfoHolder implements UserInfoHolder {

    private UserProviderService userProviderService;

    @Autowired
    public DefaultUserInfoHolder(UserProviderService userProviderService) {
        this.userProviderService = userProviderService;
    }

    private ThreadLocal<String> userIdLocal = new ThreadLocal<>();

    @Override
    public UserInfo getUser() {
        String userId = userIdLocal.get();
        Map<String, UserInfo> mapUser = userProviderService.getUsers();
        if (mapUser.containsKey(userId)) {
            return mapUser.get(userId);
        }
        return null;
    }

    @Override
    public String getUserId() {
        return userIdLocal.get();

    }

    @Override
    public void setUserId(String userId) {
        userIdLocal.set(userId);
    }

    @Override
    public void clear() {
        userIdLocal.remove();
    }
}
