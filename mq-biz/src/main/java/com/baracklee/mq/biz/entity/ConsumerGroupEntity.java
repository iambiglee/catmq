package com.baracklee.mq.biz.entity;

import java.util.Date;

public class ConsumerGroupEntity {
    private long id;
    //订阅者集合名称，唯一
    private String name;

    private String dptName;
    //TOPIC 名称集合
    private String topicNames;

    private String ownerIds;

    private String ownerName;

    private String alarmEmails;

    private String tels;

    private String ipWhiteList;

    private String ipBlackList;

    private int alarmFlag;
    //是否开启消息追踪
    private int traceFlag;

    private String remark;

    private long rbVersion;

    private long metaVersion;

    private long version;

    private String insertBy;

    private Date insertTime;

    private String updateBy;

    private Date updateTime;

    private int isActive;

    private int consumerCount;

    private String appId;

    private int consumerQuality;

    private Date metaUpdateTime;

    //1,集群模式 2，广播模式 3，代理模式
    private int mode;

    private String originName;
    //1 表示实时推送 0 表示实时推送
    private String subEnv;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDptName() {
        return dptName;
    }

    public void setDptName(String dptName) {
        this.dptName = dptName;
    }

    public String getTopicNames() {
        return topicNames;
    }

    public void setTopicNames(String topicNames) {
        this.topicNames = topicNames;
    }

    public String getOwnerIds() {
        return ownerIds;
    }

    public void setOwnerIds(String ownerIds) {
        this.ownerIds = ownerIds;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getAlarmEmails() {
        return alarmEmails;
    }

    public void setAlarmEmails(String alarmEmails) {
        this.alarmEmails = alarmEmails;
    }

    public String getTels() {
        return tels;
    }

    public void setTels(String tels) {
        this.tels = tels;
    }

    public String getIpWhiteList() {
        return ipWhiteList;
    }

    public void setIpWhiteList(String ipWhiteList) {
        this.ipWhiteList = ipWhiteList;
    }

    public String getIpBlackList() {
        return ipBlackList;
    }

    public void setIpBlackList(String ipBlackList) {
        this.ipBlackList = ipBlackList;
    }

    public int getAlarmFlag() {
        return alarmFlag;
    }

    public void setAlarmFlag(int alarmFlag) {
        this.alarmFlag = alarmFlag;
    }

    public int getTraceFlag() {
        return traceFlag;
    }

    public void setTraceFlag(int traceFlag) {
        this.traceFlag = traceFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getRbVersion() {
        return rbVersion;
    }

    public void setRbVersion(long rbVersion) {
        this.rbVersion = rbVersion;
    }

    public long getMetaVersion() {
        return metaVersion;
    }

    public void setMetaVersion(long metaVersion) {
        this.metaVersion = metaVersion;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getConsumerQuality() {
        return consumerQuality;
    }

    public void setConsumerQuality(int consumerQuality) {
        this.consumerQuality = consumerQuality;
    }

    public Date getMetaUpdateTime() {
        return metaUpdateTime;
    }

    public void setMetaUpdateTime(Date metaUpdateTime) {
        this.metaUpdateTime = metaUpdateTime;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getSubEnv() {
        return subEnv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerGroupEntity)) return false;

        ConsumerGroupEntity that = (ConsumerGroupEntity) o;

        if (getId() != that.getId()) return false;
        if (getAlarmFlag() != that.getAlarmFlag()) return false;
        if (getTraceFlag() != that.getTraceFlag()) return false;
        if (getRbVersion() != that.getRbVersion()) return false;
        if (getMetaVersion() != that.getMetaVersion()) return false;
        if (getVersion() != that.getVersion()) return false;
        if (getIsActive() != that.getIsActive()) return false;
        if (getConsumerCount() != that.getConsumerCount()) return false;
        if (getConsumerQuality() != that.getConsumerQuality()) return false;
        if (getMode() != that.getMode()) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getDptName() != null ? !getDptName().equals(that.getDptName()) : that.getDptName() != null) return false;
        if (getTopicNames() != null ? !getTopicNames().equals(that.getTopicNames()) : that.getTopicNames() != null)
            return false;
        if (getOwnerIds() != null ? !getOwnerIds().equals(that.getOwnerIds()) : that.getOwnerIds() != null)
            return false;
        if (getOwnerName() != null ? !getOwnerName().equals(that.getOwnerName()) : that.getOwnerName() != null)
            return false;
        if (getAlarmEmails() != null ? !getAlarmEmails().equals(that.getAlarmEmails()) : that.getAlarmEmails() != null)
            return false;
        if (getTels() != null ? !getTels().equals(that.getTels()) : that.getTels() != null) return false;
        if (getIpWhiteList() != null ? !getIpWhiteList().equals(that.getIpWhiteList()) : that.getIpWhiteList() != null)
            return false;
        if (getIpBlackList() != null ? !getIpBlackList().equals(that.getIpBlackList()) : that.getIpBlackList() != null)
            return false;
        if (getRemark() != null ? !getRemark().equals(that.getRemark()) : that.getRemark() != null) return false;
        if (getInsertBy() != null ? !getInsertBy().equals(that.getInsertBy()) : that.getInsertBy() != null)
            return false;
        if (getInsertTime() != null ? !getInsertTime().equals(that.getInsertTime()) : that.getInsertTime() != null)
            return false;
        if (getUpdateBy() != null ? !getUpdateBy().equals(that.getUpdateBy()) : that.getUpdateBy() != null)
            return false;
        if (getUpdateTime() != null ? !getUpdateTime().equals(that.getUpdateTime()) : that.getUpdateTime() != null)
            return false;
        if (getAppId() != null ? !getAppId().equals(that.getAppId()) : that.getAppId() != null) return false;
        if (getMetaUpdateTime() != null ? !getMetaUpdateTime().equals(that.getMetaUpdateTime()) : that.getMetaUpdateTime() != null)
            return false;
        if (getOriginName() != null ? !getOriginName().equals(that.getOriginName()) : that.getOriginName() != null)
            return false;
        return getSubEnv() != null ? getSubEnv().equals(that.getSubEnv()) : that.getSubEnv() == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getDptName() != null ? getDptName().hashCode() : 0);
        result = 31 * result + (getTopicNames() != null ? getTopicNames().hashCode() : 0);
        result = 31 * result + (getOwnerIds() != null ? getOwnerIds().hashCode() : 0);
        result = 31 * result + (getOwnerName() != null ? getOwnerName().hashCode() : 0);
        result = 31 * result + (getAlarmEmails() != null ? getAlarmEmails().hashCode() : 0);
        result = 31 * result + (getTels() != null ? getTels().hashCode() : 0);
        result = 31 * result + (getIpWhiteList() != null ? getIpWhiteList().hashCode() : 0);
        result = 31 * result + (getIpBlackList() != null ? getIpBlackList().hashCode() : 0);
        result = 31 * result + getAlarmFlag();
        result = 31 * result + getTraceFlag();
        result = 31 * result + (getRemark() != null ? getRemark().hashCode() : 0);
        result = 31 * result + (int) (getRbVersion() ^ (getRbVersion() >>> 32));
        result = 31 * result + (int) (getMetaVersion() ^ (getMetaVersion() >>> 32));
        result = 31 * result + (int) (getVersion() ^ (getVersion() >>> 32));
        result = 31 * result + (getInsertBy() != null ? getInsertBy().hashCode() : 0);
        result = 31 * result + (getInsertTime() != null ? getInsertTime().hashCode() : 0);
        result = 31 * result + (getUpdateBy() != null ? getUpdateBy().hashCode() : 0);
        result = 31 * result + (getUpdateTime() != null ? getUpdateTime().hashCode() : 0);
        result = 31 * result + getIsActive();
        result = 31 * result + getConsumerCount();
        result = 31 * result + (getAppId() != null ? getAppId().hashCode() : 0);
        result = 31 * result + getConsumerQuality();
        result = 31 * result + (getMetaUpdateTime() != null ? getMetaUpdateTime().hashCode() : 0);
        result = 31 * result + getMode();
        result = 31 * result + (getOriginName() != null ? getOriginName().hashCode() : 0);
        result = 31 * result + (getSubEnv() != null ? getSubEnv().hashCode() : 0);
        return result;
    }

    public void setSubEnv(String subEnv) {
        this.subEnv = subEnv;

    }
}
