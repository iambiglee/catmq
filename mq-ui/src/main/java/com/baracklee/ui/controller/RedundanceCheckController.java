package com.baracklee.ui.controller;


import com.baracklee.mq.biz.ui.dto.response.RedundanceCheckResponse;
import com.baracklee.ui.service.UiRedundanceCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/redundance")
public class RedundanceCheckController {
    @Autowired
    private UiRedundanceCheckService uiRedundanceCheckService;

    @RequestMapping("/checkAll")
    public RedundanceCheckResponse checkAll() {
        return uiRedundanceCheckService.checkAll();
    }

    @RequestMapping("/imitate")
    public  RedundanceCheckResponse imitate(){
        return uiRedundanceCheckService.imitate();
    }
}
