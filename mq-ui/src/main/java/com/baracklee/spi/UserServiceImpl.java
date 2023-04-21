package com.baracklee.spi;

import com.baracklee.mq.biz.dto.UserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:  BarackLee
 */
@Service
public class UserServiceImpl implements UserService {

    @Override
    public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
        return null;
    }

    @Override
    public List<String> getDpts() {
        return null;
    }

    @Override
    public String getCurrentDpt() {
        return null;
    }

    @Override
    public UserInfo findByUserId(String userId) {
        return null;
    }

    @Override
    public List<UserInfo> findByUserIds(List<String> userIds) {
        return null;
    }

    @Override
    public String getNamesByUserIds(String userIdS) {
        return null;
    }

    @Override
    public List<String> getBizTypes() {
        return null;
    }

    @Override
    public boolean login(String username, String password) {
        return false;
    }
}
