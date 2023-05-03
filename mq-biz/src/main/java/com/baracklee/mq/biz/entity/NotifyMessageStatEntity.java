package com.baracklee.mq.biz.entity;

import java.util.Date;

public class NotifyMessageStatEntity {
    private long id;
    private String key1;
    private long notifyMessageId;
    private String insertBy;
    private Date insertTime;
    private String updateBy;
    private Date updateTime;
    private int isActive;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey1() {
        return key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public long getNotifyMessageId() {
        return notifyMessageId;
    }

    public void setNotifyMessageId(long notifyMessageId) {
        this.notifyMessageId = notifyMessageId;
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
    /**
     * 字段名常量值，在构造查询map时，key值就不需要hard code了。
     * 如构造查询ID为121的查询map时，map.put(NotifyMessageStatEntity.FdId， "121");
     */

    public final static String TABLE_NAME = "notify_message_stat";

    public static String FdId = "id";

    public static String FdKey1 = "key1";

    public static String FdNotifyMessageId = "notifyMessageId";

    public static String FdInsertBy = "insertBy";

    public static String FdInsertTime = "insertTime";

    public static String FdUpdateBy = "updateBy";

    public static String FdUpdateTime = "updateTime";

    public static String FdIsActive = "isActive";

}
