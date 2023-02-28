package com.baracklee.mq.client.stat;


import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.*;
import java.io.IOException;

@Component
public class MqFilter implements Filter {
    @Resource
    private StatService statService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(servletRequest,servletResponse);
        } catch (Exception e) {
            statService.start();
            throw e;
        }
    }

    @Override
    public void destroy() {

    }
}
