package com.baracklee.mq.biz.entity;

import java.util.Date;

public class ConsumerEntity {
    private long id;

    private String ip;

    private String name;

    private String consumerGroupNames;

    private String sdkVersion;

    private String lan;

    private Date heartTime;

    private String insertBy;

    private Date insertTime;

    private String updateTime;

    private String updateBy;

    private int isActive;


    public static String FdId = "id";

    public static String FdIp = "ip";

    public static String FdName = "name";

    public static String FdConsumerGroupNames = "consumerGroupNames";

    public static String FdSdkVersion = "sdkVersion";

    public static String FdLan = "lan";

    public static String FdHeartTime = "heartTime";

    public static String FdInsertBy = "insertBy";

    public static String FdInsertTime = "insertTime";

    public static String FdUpdateBy = "updateBy";

    public static String FdUpdateTime = "updateTime";

    public static String FdIsActive = "isActive";


    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConsumerGroupNames() {
        return consumerGroupNames;
    }

    public void setConsumerGroupNames(String consumerGroupNames) {
        this.consumerGroupNames = consumerGroupNames;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getLan() {
        return lan;
    }

    public void setLan(String lan) {
        this.lan = lan;
    }

    public Date getHeartTime() {
        return heartTime;
    }

    public void setHeartTime(Date heartTime) {
        this.heartTime = heartTime;
    }

    public String getInsertBy() {
        return insertBy;
    }

    public void setInsertBy(String insertBy) {
        this.insertBy = insertBy;
    }

    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
}
