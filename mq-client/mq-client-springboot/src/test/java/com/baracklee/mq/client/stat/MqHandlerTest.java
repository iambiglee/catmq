package com.baracklee.mq.client.stat;

import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)
public class MqHandlerTest {

    @Test
    public void test() {
        MqHandler mqHandler=new MqHandler();
        mqHandler.addLifeCycleListener(null);
        mqHandler.destroy();
        mqHandler.getServer();
        mqHandler.isFailed();
        mqHandler.isRunning();
        mqHandler.isStarted();
        mqHandler.isStarting();
        mqHandler.isStopped();
        mqHandler.isStopping();
        mqHandler.removeLifeCycleListener(null);
        mqHandler.setServer(null);
    }

    @Test
    public void handleTest() throws IOException, ServletException {
        MqClientStatController mqClientStatController=new MqClientStatController();
        MqHandler mqHandler=new MqHandler();
        ReflectionTestUtils.setField(mqHandler, "mqClientStatController", mqClientStatController);

        Request request=mock(Request.class);

        //System.out.println(request.getRequestURI());
        HttpServletRequest httpServletRequest=mock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse=mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/mq/client/th");
        PrintWriter printWriter=mock(PrintWriter.class);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);
        mqHandler.handle("", request, httpServletRequest, httpServletResponse);
        verify(printWriter).write(anyString());
    }
}