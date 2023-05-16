package com.baracklee.mq.biz.ui.vo;

/**
 * @author Barack Lee
 */
public class PhysicalMachineReportVo {
    String ip;
    long msgCount;
    long avgCount;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(long msgCount) {
        this.msgCount = msgCount;
    }

    public long getAvgCount() {
        return avgCount;
    }

    public void setAvgCount(long avgCount) {
        this.avgCount = avgCount;
    }
}
