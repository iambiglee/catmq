package com.baracklee.ui.filter;

import com.baracklee.mq.biz.service.Message01Service;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.ui.util.CookieUtil;
import com.baracklee.ui.util.DesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Order(1)
@WebFilter(filterName = "WebAuthFilter",urlPatterns = "/*")
public class AuthFilter implements Filter {
    Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Message01Service message01Service;
    private UserInfoHolder userInfoHolder;

    @Autowired
    public AuthFilter(Message01Service message01Service, UserInfoHolder userInfoHolder) {
        this.message01Service = message01Service;
        this.userInfoHolder = userInfoHolder;
        skipUrlLst= Arrays.asList("/login", ".js", ".css", ".jpg", ".ico",".woff", ".png", "/auth" ,"/cat","/hs","/message/getByTopic");

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String uri = request.getRequestURI();
        if (skipUri(uri)) {
            chain.doFilter(request, response);
        } else {
            try {
                Cookie cookie = CookieUtil.getCookie(request, "userSessionId");
                if (cookie == null) {
                    response.sendRedirect("/login");
                } else {
                    String userId = DesUtil.decrypt(cookie.getValue());
                    userInfoHolder.setUserId(userId);
                    chain.doFilter(request, response);
                }

            } catch (Exception e) {
                log.error("login fail", e);
                response.sendRedirect("/login");
            } finally {
                message01Service.clearDbId();
                userInfoHolder.clear();
            }
        }
    }
    private List<String> skipUrlLst = new ArrayList<>();

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
    private boolean skipUri(String uri) {
        for(String t : skipUrlLst){
            if(uri.contains(t)){
                return true;
            }
        }
        return false;
    }
}
