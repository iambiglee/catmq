package com.baracklee.mq.client.event;

import com.baracklee.mq.biz.event.PostHandleListener;
import com.baracklee.mq.biz.event.PreHandleListener;

import java.util.ArrayList;
import java.util.List;

public class MqEvent {

    //在消息的不同阶段和生命周期添加不同的拦截器，增强器，方便用户对消息进行处理
    //类似于Spring 生成 Bean的各种处理
    private List<Runnable> initCompleted = new ArrayList<>();
    private PreHandleListener preHandleListener = null;
    private PostHandleListener postHandleListener = null;
    private List<Runnable> registerCompleted = new ArrayList<>();
    public List<Runnable> getInitCompleted() {
        return initCompleted;
    }

    public void setInitCompleted(List<Runnable> initCompleted) {
        this.initCompleted = initCompleted;
    }

    public PreHandleListener getPreHandleListener() {
        return preHandleListener;
    }

    public void setPreHandleListener(PreHandleListener preHandleListener) {
        this.preHandleListener = preHandleListener;
    }

    public PostHandleListener getPostHandleListener() {
        return postHandleListener;
    }

    public void setPostHandleListener(PostHandleListener postHandleListener) {
        this.postHandleListener = postHandleListener;
    }

    public List<Runnable> getRegisterCompleted() {
        return registerCompleted;
    }

    public void setRegisterCompleted(List<Runnable> registerCompleted) {
        this.registerCompleted = registerCompleted;
    }
}
