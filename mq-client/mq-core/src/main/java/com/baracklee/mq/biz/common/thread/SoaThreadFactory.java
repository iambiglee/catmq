package com.baracklee.mq.biz.common.thread;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SoaThreadFactory implements ThreadFactory {
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    private final AtomicLong threadNumber = new AtomicLong(1);
    private final String namePrefix;
    private int priority = 0;
    private final boolean daemon;
    private static final ThreadGroup THREAD_GROUP=new ThreadGroup("Mq");

    public static ThreadGroup getThreadGroup(){return THREAD_GROUP;}

    public static ThreadFactory create(String namePrefix,boolean daemon){
        return new SoaThreadFactory(namePrefix,daemon);
    }

    public static ThreadFactory create(String namePrefix){
        return create(namePrefix,true);
    }

    public static ThreadFactory create(String namePrefix,int priority,boolean daemon){
        return new SoaThreadFactory(namePrefix,priority,daemon);
    }
    private SoaThreadFactory(String namePrefix,boolean daemon){
        this.namePrefix=namePrefix;
        this.daemon=daemon;
    }
    private SoaThreadFactory(String namePrefix,int priority,boolean daemon){
        this.daemon=daemon;
        this.namePrefix=namePrefix;
        this.priority=priority;
    }


    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(THREAD_GROUP, runnable, THREAD_GROUP.getName() + "-" + namePrefix + threadNumber.getAndIncrement());
        thread.setDaemon(daemon);
        if (priority>0){
         thread.setPriority(priority);
        }
        return thread;
    }
}
