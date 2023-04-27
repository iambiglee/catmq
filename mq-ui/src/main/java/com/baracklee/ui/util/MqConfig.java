package com.baracklee.ui.util;

import com.alibaba.druid.support.http.StatViewServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    public ServletRegistrationBean druidStatViewServlet(){

        // org.springframework.boot.context.embedded.ServletRegistrationBean提供类的进行注册.

        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(),
                "/druid/*");

        // 添加初始化参数：initParams
        //
        // //白名单：
        //
        // servletRegistrationBean.addInitParameter("allow","127.0.0.1");
        //
        // //IP黑名单 (存在共同时，deny优先于allow) : 如果满足deny的话提示:Sorry, you are not
        // permitted to view this page.
        //
        // servletRegistrationBean.addInitParameter("deny","192.168.1.73");

        // 登录查看信息的账号密码.

        servletRegistrationBean.addInitParameter("loginUsername", "admin");

        servletRegistrationBean.addInitParameter("loginPassword", "admin");

        // 是否能够重置数据.

        servletRegistrationBean.addInitParameter("resetEnable", "false");

        return servletRegistrationBean;

    }

}
