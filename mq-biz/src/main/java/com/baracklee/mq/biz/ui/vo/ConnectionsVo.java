package com.baracklee.mq.biz.ui.vo;

/**
 * @author Barack Lee
 */
public class ConnectionsVo {
    private String ip;
    private String maxConnection;
    private String curConnection;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(String maxConnection) {
        this.maxConnection = maxConnection;
    }

    public String getCurConnection() {
        return curConnection;
    }

    public void setCurConnection(String curConnection) {
        this.curConnection = curConnection;
    }
}
