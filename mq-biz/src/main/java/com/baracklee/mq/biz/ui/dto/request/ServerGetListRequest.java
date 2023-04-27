package com.baracklee.mq.biz.ui.dto.request;

import com.baracklee.mq.biz.dto.request.BaseUiRequst;

public class ServerGetListRequest extends BaseUiRequst {
    private String statusFlag;

    private String serverVersion;

    public String getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(String statusFlag) {
        this.statusFlag = statusFlag;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }
}
