package com.baracklee.ui.controller;

import com.baracklee.ui.spi.UserService;
import com.baracklee.ui.util.DesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping("/auth")
public class AuthController {
private UserService userService;
    Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @RequestMapping(value = "/mqlogin",method= RequestMethod.POST)
    public Integer login(@RequestParam String userName, @RequestParam String userPass, HttpServletResponse response){
        boolean flag = userService.login(userName, userPass);
        if (flag){
            try {
                //DesUtil desUtil= new DesUtil();
                Cookie ck=new Cookie("userSessionId", DesUtil.encrypt(userName));
                ck.setMaxAge(60*600);
                ck.setPath("/");
                response.addCookie(ck);
            } catch (Exception e) {
                return 0;
            }
        }
        return flag ? 1 : 0;
    }

}
