package com.baracklee.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ViewController {
    @RequestMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "login";
    }

}
