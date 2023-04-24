package com.baracklee.ui.controller;

import com.baracklee.ui.spi.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping("/auth")
public class AuthController {
private UserService service;
    Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

}
