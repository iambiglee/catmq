package com.baracklee.mq.biz.dto.base;

public class ConsumerGroupMetaDto {
    private String name;

    private long reVersion;

    private long metaVersion;

    private long version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getReVersion() {
        return reVersion;
    }

    public void setReVersion(long reVersion) {
        this.reVersion = reVersion;
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
}
