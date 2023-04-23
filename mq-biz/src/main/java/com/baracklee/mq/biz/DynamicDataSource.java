package com.baracklee.mq.biz;

import com.baracklee.mq.biz.service.Message01Service;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    private Message01Service message01Service;

    public DynamicDataSource(Message01Service message01Service) {
        this.message01Service = message01Service;
    }

    public DataSource getDataSource(){
        return message01Service.getDataSource();
    }


    @Override
    protected Object determineCurrentLookupKey() {
        return null;
    }

    protected DataSource determineTargetDataSource() {
        return message01Service.getDataSource();
    }
    public void afterPropertiesSet() {}
}
