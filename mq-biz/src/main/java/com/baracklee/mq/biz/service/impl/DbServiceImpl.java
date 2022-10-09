package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.DbRepository;
import com.baracklee.mq.biz.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

public class DbServiceImpl implements DbService {

    @Resource
    private DbRepository dbRepository;
    @Override
    public Date getDbTime() {
        return dbRepository.getDbTime();
    }

    @Override
    public String getMaxConnectionsCount() {
        Map<String, String> map = dbRepository.getMaxConnectionsCount();
        if (map.size() == 0)
            return "0";
        else
            return map.get("Value");
    }

    @Override
    public Integer getConnectionsCount() {
        Integer count = dbRepository.getConnectionsCount();
        if (count == null)
            return 0;
        return count;
    }
}
