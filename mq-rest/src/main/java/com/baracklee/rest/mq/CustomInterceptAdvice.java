package com.baracklee.rest.mq;

import com.baracklee.MqConstanst;
import com.baracklee.mq.biz.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

import java.lang.invoke.MethodHandles;

/**
 * Author:  BarackLee
 */
@ControllerAdvice
public class CustomInterceptAdvice {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(value = { Exception.class })
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BaseResponse handle(Exception e, HandlerMethod m) {
        logger.info("CustomInterceptAdvice handle exception {}, method: {}", e.getMessage(), m.getMethod().getName());
        BaseResponse response = new BaseResponse();
        response.setCode(MqConstanst.NO);
        response.setSuc(false);
        response.setMsg(e.getMessage());
        return response;
    }
}
