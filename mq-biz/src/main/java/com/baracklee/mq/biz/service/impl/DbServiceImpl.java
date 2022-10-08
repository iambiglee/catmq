package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.DbRepository;
import com.baracklee.mq.biz.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Date;

public class DbServiceImpl implements DbService {

    @Resource
    private DbRepository dbRepository;
    @Override
    public Date getDbTime() {
        return dbRepository.getDbTime();
    }

    @Override
    public String getMaxConnectionsCount() {
        return null;
    }

    @Override
    public Integer getConnectionsCount() {
        return null;
    }
}
