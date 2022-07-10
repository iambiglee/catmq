package com.baracklee.mq.biz.service;

public interface CacheUpdateService {
void updateCache();

void forceUpdateCache();

String getCacheJson();
}

