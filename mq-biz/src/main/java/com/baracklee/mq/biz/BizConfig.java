package com.baracklee.mq.biz;

import com.baracklee.mq.biz.common.trace.TraceFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
@ComponentScan(basePackageClasses = {BizConfig.class})
public class BizConfig {
    private Environment environment;

    @PostConstruct
    private void init(){
        TraceFactory.setTraceCheck(name -> {
            if (environment==null){return false;}
            else {
                return "1".equals(environment.getProperty("mq.trace.enable", "1")) && "1".equals(environment.getProperty(name, "1"));
            }
        });
    }


}
