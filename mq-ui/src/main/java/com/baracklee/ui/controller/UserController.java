package com.baracklee.ui.controller;

import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.ui.dto.response.*;
import com.baracklee.ui.spi.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:  BarackLee
 */
@RestController
@RequestMapping("/user")
public class UserController {

    Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private UserService userService;

    private UserInfoHolder userInfoHolder;

    @Autowired
    public UserController(UserService userService, UserInfoHolder userInfoHolder) {
        this.userService = userService;
        this.userInfoHolder = userInfoHolder;
    }

    @GetMapping("/search")
    public UserSearchResponse searchUsers(String keyword, int offset, int limit) {
        if (StringUtils.isEmpty(keyword)) {
            return new UserSearchResponse(0L, null);
        }
        List<UserInfo> userInfos = userService.searchUsers(keyword, offset, limit);
        return new UserSearchResponse((long) userInfos.size(), userInfos);
    }

    @GetMapping("/getDepartments")
    public DepartmentsGetResponse getDepartments() {
        List<String> dpts = userService.getDpts();
        return new DepartmentsGetResponse((long) dpts.size(), dpts);
    }

    @RequestMapping("/getDepartmentsBySearch")
    public DepartmentsGetResponse getDepartmentsBySearch(String keyword, int offset, int limit) {
        List<String> dpts = userService.getDpts();
        List<String> dptList = new ArrayList<>();
        for (String dpt : dpts) {
            if (dpt.indexOf(keyword) != -1) {
                dptList.add(dpt);
            }
        }
        return new DepartmentsGetResponse((long) dptList.size(), dptList);
    }
    @GetMapping("/getCurrentDpt")
    public UserGetCurrentDptResponse getCurrentDpt() {
        return new UserGetCurrentDptResponse(userService.getCurrentDpt());
    }

    @GetMapping("/getCurrentUser")
    public UserGetCurrentUserResponse getCurrentUser() {
        return new UserGetCurrentUserResponse(userInfoHolder.getUser());
    }

    @GetMapping("/getBizType")
    public UserGetBizTypesResponse getBizTypes() {
        List<String> bizTypes = userService.getBizTypes();
        return new UserGetBizTypesResponse((long) bizTypes.size(), bizTypes);
    }

    @GetMapping("/getByUserIds")
    public UserGetByUserIdsResponse getByUserIds(String userIds) {
        return new UserGetByUserIdsResponse(userService.getNamesByUserIds(userIds));
    }


}
