package com.baracklee.mq.client.stat;

import com.baracklee.mq.client.MqSpringUtil;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class MqHandler implements Handler {

    private MqClientStatController mqClientStatController;
    private Map<String, Method> maps = new HashMap<>();

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
        if (maps.size() == 0) {
            synchronized (this) {
                if (maps.size() == 0) {
                    mqClientStatController= MqSpringUtil.getBean(MqClientStatController.class);
                    if(mqClientStatController==null) {
                        mqClientStatController=new MqClientStatController();
                    }
                    initMap();
                }
            }
        }
        if (maps.containsKey(request.getRequestURI())) {
            response.setContentType("text/html;charset=UTF-8");
            StringBuilder sbHtml = new StringBuilder();
            sbHtml.append(
                    "<!doctype html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>");

            try {
                sbHtml.append(maps.get(request.getRequestURI()));
                sbHtml.append("</body></html>");
                response.getWriter().write(sbHtml.toString());
                response.flushBuffer();
            } catch (Exception e) {
            }
        }
    }

    private void initMap() {
        Method[] methods = MqClientStatController.class.getMethods();
        for (Method method : methods) {
            GetMapping annotation = method.getAnnotation(GetMapping.class);
            if(annotation!=null){
                maps.put(annotation.value()[0],method);
            }
        }
    }

    @Override
    public void setServer(Server server) {

    }

    @Override
    public Server getServer() {
        return null;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStarting() {
        return false;
    }

    @Override
    public boolean isStopping() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public void addLifeCycleListener(Listener listener) {

    }

    @Override
    public void removeLifeCycleListener(Listener listener) {

    }
}
