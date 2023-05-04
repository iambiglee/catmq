package com.baracklee.mq.biz.ui.vo;

import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import org.springframework.beans.BeanUtils;

/**
 * @author Barack Lee
 */
public class ConsumerGroupTopicVo extends ConsumerGroupTopicEntity {
    private int role;
    public ConsumerGroupTopicVo(ConsumerGroupTopicEntity consumerGroupTopicEntity){
        BeanUtils.copyProperties(consumerGroupTopicEntity,this);
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }
}
