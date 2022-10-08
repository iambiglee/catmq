package com.baracklee.mq.biz.service;

import java.util.Date;

public interface DbService {

    Date getDbTime();
    String getMaxConnectionsCount();

    Integer getConnectionsCount();


}
