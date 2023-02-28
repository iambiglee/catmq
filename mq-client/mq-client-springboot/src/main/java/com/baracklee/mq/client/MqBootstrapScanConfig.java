package com.baracklee.mq.client;


import com.baracklee.mq.client.stat.MqFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
@Configuration
@ComponentScan(basePackageClasses = { MqBootstrapScanConfig.class })
public class MqBootstrapScanConfig {
    @Bean("clientMqFilter")
    public FilterRegistrationBean clientMqFilter(MqFilter mqFilter) {
        FilterRegistrationBean openApiFilter = new FilterRegistrationBean();
        openApiFilter.setFilter(mqFilter);
        openApiFilter.addUrlPatterns("/mq/client/*");
        return openApiFilter;
    }
}
