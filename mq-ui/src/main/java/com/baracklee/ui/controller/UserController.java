package com.baracklee.ui.controller;

import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.spi.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

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
}
