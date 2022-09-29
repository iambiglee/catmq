package com.baracklee.mq.biz.common.trace;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TraceMessageItem {
    public String msg;
    public String status;
    public String startTime;
    public String endTime;

    private String setDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS");
        return sdf.format(new Date());
    }
    private void start(){this.startTime=setDate();}
    public TraceMessageItem(){start();};
}
