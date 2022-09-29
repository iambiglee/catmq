package com.baracklee.mq.biz.common.trace;

import com.baracklee.mq.biz.common.util.Util;

import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TraceMessage {
    private volatile int counter=0;
    private volatile TraceMessageItem[] data=new TraceMessageItem[100];
    private String name;
    private transient ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
    public TraceMessage(String name){
        this.name=name;
    }

//    public void add(TraceMessageItem traceMessageItem) {
//        if (!TraceFactory.isEnabled(name)) {
//
//            return;
//        }
//        if (Util.isEmpty(traceMessageItem.status)) {
//            traceMessageItem.status = "none";
//        }
//        traceMessageItem.endTime = Util.formateDate(new Date(), Util.SSS_FORMATE);
//        doAdd(traceMessageItem);
//    }

}
