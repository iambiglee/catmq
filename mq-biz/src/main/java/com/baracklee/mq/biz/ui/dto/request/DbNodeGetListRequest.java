package com.baracklee.mq.biz.ui.dto.request;

import com.baracklee.mq.biz.dto.request.BaseUiRequst;

/**
 * @Author： Barack Lee
 */
public class DbNodeGetListRequest extends BaseUiRequst {

    String id;
    String name;
    String ip;
    String readOnly;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

}
