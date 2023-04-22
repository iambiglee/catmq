package com.baracklee.rest.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class RestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class);
    }
}
