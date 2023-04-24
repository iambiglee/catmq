package com.baracklee.ui.spi;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.Organization;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.service.UserInfoHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  BarackLee
 */
@Service
public class UserServiceImpl implements UserService {

    private UserProviderService userProviderService;
    private UserInfoHolder userInfoHolder;

    private SoaConfig soaConfig;

    @Autowired
    public UserServiceImpl(UserProviderService userProviderService, UserInfoHolder userInfoHolder, SoaConfig soaConfig) {
        this.userProviderService = userProviderService;
        this.userInfoHolder = userInfoHolder;
        this.soaConfig = soaConfig;
    }

    @Override
    public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
        Map<String, UserInfo> users = userProviderService.getUsers();
        if (StringUtils.isEmpty(keyword)) {
            return Arrays.asList(userInfoHolder.getUser());
        } else {
            List<UserInfo> userRs = new ArrayList<>();
            for (UserInfo userInfo : users.values()) {
                if (userInfo.getName().indexOf(keyword) != -1 || userInfo.getUserId().indexOf(keyword) != -1) {
                    userRs.add(userInfo);
                }
            }
            if (offset + limit > userRs.size()) {
                limit = userRs.size() - offset;
            }
            return userRs.subList(offset, limit);
        }
    }

    @Override
    public List<String> getDpts() {
        Map<String, Organization> orgs = userProviderService.getOrgs();
        return new ArrayList<>(orgs.keySet());
    }

    @Override
    public String getCurrentDpt() {
        String userId = userInfoHolder.getUserId();
        UserInfo userInfo = findByUserId(userId);
        return userInfo.getDepartment();
    }

    @Override
    public UserInfo findByUserId(String userId) {
        Map<String, UserInfo> users = userProviderService.getUsers();
        if (users.containsKey(userId)) {
            return users.get(userId);
        }
        return null;
    }

    @Override
    public List<UserInfo> findByUserIds(List<String> userIds) {
        Map<String, UserInfo> users = userProviderService.getUsers();
        List<UserInfo> userRs = new ArrayList<>();
        for (UserInfo userInfo : users.values()) {
            if (userIds.contains(userInfo.getUserId()) || userIds.contains(userInfo.getName())) {
                userRs.add(userInfo);
            }
        }
        return userRs;
    }

    @Override
    public String getNamesByUserIds(String userIds) {
        List<UserInfo> userInfoList = findByUserIds(Arrays.asList(userIds.split(",")));
        List<String> userIdList = userInfoList.stream().map(UserInfo::getUserId).collect(Collectors.toList());
        List<String> nameList = userInfoList.stream().map(UserInfo::getName).collect(Collectors.toList());
        return String.join(",", userIdList) + ";" + String.join(",", nameList);
    }

    @Override
    public List<String> getBizTypes() {
        return soaConfig.getBizTypes();
    }

    @Override
    public boolean login(String username, String password) {
        return userProviderService.login(username,password);
    }
}
