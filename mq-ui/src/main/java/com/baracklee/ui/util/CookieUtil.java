package com.baracklee.ui.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    public static String getUserName(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        String userName="";
        if(cookies!=null){
            Cookie cookie = getCookie(request, "userSessionId");
            if (cookie == null) {
                return "";
            }
            try {
                return DesUtil.decrypt(cookie.getValue());
            } catch (Exception e) {
                return "";
            }

        }
        return userName;
    }

    private static Cookie getCookie(HttpServletRequest request, String userSessionId) {
        Cookie[] cookies = request.getCookies();
        if (null != cookies){
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(userSessionId)){
                    return cookie;
                }
            }
        }
        return null;
    }
}
