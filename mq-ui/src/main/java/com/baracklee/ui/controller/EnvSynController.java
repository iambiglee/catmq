package com.baracklee.ui.controller;


import com.baracklee.mq.biz.ui.dto.response.EnvSynAllResponse;
import com.baracklee.mq.biz.ui.dto.response.EnvSynGenerateResponse;
import com.baracklee.ui.service.EnvSynService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author： Barack Lee
 * @Date：2021/7/18 13:33
 */
@RestController
@RequestMapping("/envSyn")
public class EnvSynController {
    @Autowired
    private EnvSynService envSynService;
    @RequestMapping("/generateAll")
    public EnvSynGenerateResponse generateAll(@RequestParam(name = "synType") String synType) {
        return envSynService.generateAll(synType);
    }

    @RequestMapping("/synAll")
    public EnvSynAllResponse synAll(@RequestParam(name = "synType") String synType, @RequestParam(name = "synMessage") String synMessage) {
        return envSynService.synAll(synType,synMessage);
    }

}
