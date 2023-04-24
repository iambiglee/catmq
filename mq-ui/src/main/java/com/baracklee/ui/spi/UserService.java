package com.baracklee.ui.spi;

import com.baracklee.mq.biz.dto.UserInfo;

import java.util.List;

/**
 * Author:  BarackLee
 */

public interface UserService {

    List<UserInfo> searchUsers(String keyword, int offset, int limit);

    List<String> getDpts();

    String getCurrentDpt();

    UserInfo findByUserId(String userId);

    List<UserInfo> findByUserIds(List<String> userIds);

    String getNamesByUserIds(String userIdS);

    List<String> getBizTypes();

    boolean login(String username, String password);
}
