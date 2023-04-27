package com.baracklee.ui.controller;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.service.*;
import com.baracklee.ui.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ViewController {

    @Autowired
    private DbNodeService dbNodeService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private ConsumerGroupService consumerGroupService;

    @Autowired
    private RoleService roleService;
    @Autowired
    private QueueService queueService;
    @Autowired
    private SoaConfig soaConfig;
    @Autowired
    private Environment env;
    @Autowired
    private UserInfoHolder userInfoHolder;

    @Autowired
    private ServerService serverService;

    Map<String, String> keysMap = new HashMap<>();



    {
        keysMap.put("defaultTopicThreadSize", SoaConfig.env_getConsumerGroupTopicThreadSize_key);
        keysMap.put("defaultTopicRetryCount", SoaConfig.env_getConsumerGroupTopicRetryCount_key);
        keysMap.put("defaultTopicLag", SoaConfig.env_getConsumerGroupTopicLag_key);
        keysMap.put("defaultTopicDelayProcessTime", SoaConfig.env_getDelayProcessTime_key);
        keysMap.put("defaultPullBatchSize", SoaConfig.env_getPullBatchSize_key);
        keysMap.put("defaultConsumerBatchSize", SoaConfig.env_getConsumerBatchSize_key);
        keysMap.put("defaultTopicDelayPullTime", SoaConfig.env_getMinDelayPullTime_key);
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "login";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("userSessionId", null);
        // 将cookie的有效期设置为0，命令浏览器删除该cookie
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/logout";
    }
    @RequestMapping("/index")
    public String index(HttpServletRequest request,Model model){
        model.addAttribute("userName", CookieUtil.getUserName(request));
        model.addAttribute("roleName", roleService.getRoleName(userInfoHolder.getUserId()));
        model.addAttribute("sdkVersion", soaConfig.getSdkVersion());
        int onlineServerNum = serverService.getOnlineServerNum();
        int role = roleService.getRole(userInfoHolder.getUserId());
        model.addAttribute("userRole",role);
        if (soaConfig.isPro()){
            model.addAttribute("proEnv",1);
        }else {
            model.addAttribute("proEnv",0);
        }
        model.addAttribute("onLineServerNum","在线server数量："+onlineServerNum);
        return "index";
    }

    @RequestMapping("/")
    public String first(HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "redirect:/index";
    }


}
