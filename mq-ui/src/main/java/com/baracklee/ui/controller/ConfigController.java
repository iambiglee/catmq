package com.baracklee.ui.controller;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.dto.request.ConfigDto;
import com.baracklee.ui.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author Barack Lee
 */
@Controller
public class ConfigController {
    @Autowired
    ConfigService configService;

    /**
     * 显示内部配置项的页面
     *
     * @param model
     * @return
     */
    @GetMapping("/config/soaConfig")
    public String getConfig(Model model) {
        return "config/soaConfig";
    }


    @RequestMapping("/config/soaConfig/data")
    @ResponseBody
    public BaseUiResponse<List<ConfigDto>> getConfigData(){
        return configService.getConfigData();
    }


}
