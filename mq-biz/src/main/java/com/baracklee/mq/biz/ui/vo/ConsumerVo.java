package com.baracklee.mq.biz.ui.vo;

import com.baracklee.mq.biz.entity.ConsumerEntity;
import org.springframework.beans.BeanUtils;

/**
 * @author Barack Lee
 */
public class ConsumerVo extends ConsumerEntity {
    private int role;
    private String ownerIds;
    private String ownerNames;

    public ConsumerVo(ConsumerEntity consumerEntity) {
        BeanUtils.copyProperties(consumerEntity, this);
    }

    public String getOwnerNames() {
        return ownerNames;
    }

    public void setOwnerNames(String ownerNames) {
        this.ownerNames = ownerNames;
    }

    public String getOwnerIds() {
        return ownerIds;
    }

    public void setOwnerIds(String ownerIds) {
        this.ownerIds = ownerIds;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }
}
