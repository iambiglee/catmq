package com.baracklee.mq.client.stat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * @Author： Barack Lee
 */
@RunWith(JUnit4.class)
public class MqFilterTest {

    @Test
    public void doFilterTest() throws IOException, ServletException {
        StatService statService=mock(StatService.class);
        MqFilter mqFilter=new MqFilter();
        ReflectionTestUtils.setField(mqFilter, "statService", statService);

        ServletRequest servletRequest=mock(ServletRequest.class);
        ServletResponse servletResponse=mock(ServletResponse.class);
        FilterChain filterChain=mock(FilterChain.class);
        mqFilter.doFilter(servletRequest, servletResponse, filterChain);
        //verify(filterChain).doFilter(eq(servletRequest), eq(servletResponse));
        verify(filterChain).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void doFilterErrorTest() throws IOException, ServletException {
        StatService statService=mock(StatService.class);
        MqFilter mqFilter=new MqFilter();
        ReflectionTestUtils.setField(mqFilter, "statService", statService);

        ServletRequest servletRequest=mock(ServletRequest.class);
        ServletResponse servletResponse=mock(ServletResponse.class);
        FilterChain filterChain=mock(FilterChain.class);

        doThrow(new RuntimeException()).when(filterChain).doFilter(servletRequest, servletResponse);
        boolean rs=false;
        try {
            mqFilter.doFilter(servletRequest, servletResponse, filterChain);
        } catch (Exception e) {
            rs=true;
        }
        verify(statService).start();
        assertEquals(true, rs);

    }
}