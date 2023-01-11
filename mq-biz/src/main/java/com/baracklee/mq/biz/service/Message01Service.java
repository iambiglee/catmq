package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.entity.Message01Entity;

public interface Message01Service {
    void setDbId(long dbNodeId);

    Message01Entity getMaxIdMsg(String tbName);

    String getDbName();

    long getMaxId(String tbName);
}
