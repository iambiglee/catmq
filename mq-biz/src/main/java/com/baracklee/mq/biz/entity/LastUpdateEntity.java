package com.baracklee.mq.biz.entity;

import java.util.Date;

public class LastUpdateEntity {
    private volatile long maxId;

    private volatile Date lastDate = new Date();

    private volatile long count =0;

    public long getMaxId() {
        return maxId;
    }

    public void setMaxId(long maxId) {
        this.maxId = maxId;
    }

    public Date getLastDate() {
        return lastDate;
    }

    public void setLastDate(Date lastDate) {
        this.lastDate = lastDate;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
