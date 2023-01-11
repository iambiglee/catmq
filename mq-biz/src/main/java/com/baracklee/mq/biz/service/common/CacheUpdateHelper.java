package com.baracklee.mq.biz.service.common;

import com.baracklee.mq.biz.common.util.SpringUtil;
import com.baracklee.mq.biz.service.CacheUpdateService;

import java.util.Map;

public class CacheUpdateHelper {

    public static void updateCache(){
        Map<String, CacheUpdateService> cacheUpdateServices = SpringUtil.getBeans(CacheUpdateService.class);
        if(cacheUpdateServices!=null){
            cacheUpdateServices.values().forEach(CacheUpdateService::updateCache);
        }

    }
}
