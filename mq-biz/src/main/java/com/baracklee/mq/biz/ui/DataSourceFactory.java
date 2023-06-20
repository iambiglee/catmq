package com.baracklee.mq.biz.ui;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author Barack Lee
 */
public interface DataSourceFactory {
    DruidDataSource createDataSource();

}
