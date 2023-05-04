package com.baracklee.mq.biz.ui.vo;

import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import org.springframework.beans.BeanUtils;

/**
 * @author Barack Lee
 */
public class ConsumerGroupVo extends ConsumerGroupEntity {
    private int role;

    public ConsumerGroupVo(ConsumerGroupEntity consumerGroupEntity){
        BeanUtils.copyProperties(consumerGroupEntity,this);
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }
}
