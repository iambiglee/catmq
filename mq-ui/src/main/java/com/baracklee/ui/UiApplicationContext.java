package com.baracklee.ui;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ServletComponentScan
public class UiApplicationContext {
    public static void main(String[] args) {
        SpringApplication.run(UiApplicationContext.class);
    }
    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }

}
