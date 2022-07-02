package com.baracklee.mq.biz.dto;

import com.baracklee.mq.biz.common.util.IPUtil;

public class BaseResponse {
    private boolean isSuc;
    private String code;
    private String msg;
    private long time;
    private String serverIp= IPUtil.getLocalIP();

    public boolean isSuc() {
        return isSuc;
    }

    public void setSuc(boolean suc) {
        isSuc = suc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
}
