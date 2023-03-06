package com.baracklee.mq.biz.common.plugin;

import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

public class DruidConnectionFilter extends FilterEventAdapter {
    private String ip;
    private int hour=0;
    public DruidConnectionFilter(String ip) {
        this.ip = ip;
        hour=(new Date()).getHours();
        hour=hour-hour%3;
    }

    @Override
    public ConnectionProxy connection_connect(FilterChain chain, Properties info) throws SQLException {
        try {
            ConnectionProxy connectionProxy = super.connection_connect(chain, info);
            return connectionProxy;
        } catch (Exception e) {
            // transaction.addData("url", chain.getDataSource().getUrl());
            throw e;
        }

    }

    @Override
    public void connection_close(FilterChain chain, ConnectionProxy connection) throws SQLException {
        try {
            super.connection_close(chain, connection);
        } catch (Exception e) {
            // transaction.addData("url", chain.getDataSource().getUrl());
            throw e;
        }
    }

}
