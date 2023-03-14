package com.baracklee.mq.biz.service;

import java.util.List;

public interface ServerService {
    List<String> getBrokerUrlCache();
    int getOnlineServerNum();
    void batchUpdate(List<Long> serverIds,int serverStatus);

}
