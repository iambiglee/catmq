package com.baracklee.ui.spi;

import com.baracklee.mq.biz.dto.Organization;
import com.baracklee.mq.biz.dto.UserInfo;

import java.util.Map;

/**
 * Author:  BarackLee
 */
public interface UserProviderService {
    //获取部门
    Map<String, Organization> getOrgs() ;
    //获取所有用户信息
    Map<String, UserInfo> getUsers();
    boolean login(String username, String password);
}
