package com.baracklee.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class UiApplicationContext {
    public static void main(String[] args) {
        SpringApplication.run(UiApplicationContext.class);
    }

}
