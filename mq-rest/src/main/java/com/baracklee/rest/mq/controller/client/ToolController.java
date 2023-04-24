package com.baracklee.rest.mq.controller.client;

import com.baracklee.mq.biz.common.util.EmailUtil;
import com.baracklee.mq.biz.common.util.IPUtil;
import com.baracklee.mq.biz.dto.BaseResponse;
import com.baracklee.mq.biz.dto.MqConstanst;
import com.baracklee.mq.biz.dto.client.LogRequest;
import com.baracklee.mq.biz.dto.client.OpLogRequest;
import com.baracklee.mq.biz.dto.client.SendMailRequest;
import com.baracklee.mq.biz.dto.client.UpdateMetaRequest;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.EmailService;
import com.baracklee.mq.biz.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(MqConstanst.TOOLPRE)
public class ToolController {

    private Environment env;

    private LogService logService;

    private ConsumerGroupService consumerGroupService;

    private EmailService emailService;

    private EmailUtil emailUtil;

    @Autowired
    public ToolController(Environment env,
                          LogService logService,
                          ConsumerGroupService consumerGroupService,
                          EmailService emailService,
                          EmailUtil emailUtil) {
        this.env = env;
        this.logService = logService;
        this.consumerGroupService = consumerGroupService;
        this.emailService = emailService;
        this.emailUtil = emailUtil;
    }


    @PostMapping("/addLog")
    public void addLog(@RequestBody LogRequest request) {
        logService.addConsumerLog(request);
    }

    @GetMapping("/mtest")
    public void mtest() {
        System.out.println("------------------------hello--------------");
        emailUtil.sendErrorMail("test", "test");
    }

    @PostMapping("/addOpLog")
    public BaseResponse addOpLog(@RequestBody OpLogRequest request) {
        logService.addOpLog(request);
        BaseResponse response = new BaseResponse();
        response.setSuc(true);
        return response;
    }

    @PostMapping("/sendMail")
    public BaseResponse sendMail(@RequestBody SendMailRequest request) {
        emailService.sendConsumerMail(request);
        BaseResponse response = new BaseResponse();
        response.setSuc(true);
        return response;
    }

    @PostMapping("/rb")
    public BaseResponse rb(@RequestBody UpdateMetaRequest request) {
        if (request != null&&"1".equals(env.getProperty("mq.client.rb", "0"))) {
            consumerGroupService.notifyRbByNames(request.getConsumerGroupName());
        }
        BaseResponse response = new BaseResponse();
        response.setSuc(true);
        return response;
    }

    @RequestMapping("/getIp")
    public String getIp() {
        return IPUtil.getLocalIP();
    }

    @GetMapping("/th")
    public String th() {
        StringBuilder rs = new StringBuilder();
        Map<Thread.State, Integer> state = new HashMap<>();
        for (Map.Entry<Thread, StackTraceElement[]> t1 : Thread.getAllStackTraces().entrySet()) {
            Thread thread = t1.getKey();
            StackTraceElement[] stackTraceElements = t1.getValue();
            // if (thread.equals(Thread.currentThread())) {
            // continue;
            // }
            state.putIfAbsent(thread.getState(), 0);
            state.put(thread.getState(), state.get(thread.getState()) + 1);
            rs.append("\n<br/>线程名称：" + thread.getName() + ",线程id:" + thread.getId() + ",16进制为："
                    + Long.toHexString(thread.getId()) + "，线程状态：" + thread.getState() + "<br/>\n");
            for (StackTraceElement st : stackTraceElements) {
                rs.append(st.toString() + "<br/>\n");
            }
        }
        StringBuilder rs1 = new StringBuilder();
        for (Map.Entry<Thread.State, Integer> t1 : state.entrySet()) {
            rs1.append("线程状态：" + t1.getKey() + ",数量：" + t1.getValue() + "<br/>\n");
        }
        return rs1.toString() + rs.toString();
    }

}
