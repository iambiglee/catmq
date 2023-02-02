package com.baracklee.mq.client.config;

import com.baracklee.mq.biz.common.util.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerGroupVo {

    //meta 就是单纯的记录coustmer group 的name 和广播模式的name
    private ConsumerGroupMetaVo meta;
    private Map<String, ConsumerGroupTopicVo> topics;

    public ConsumerGroupVo() {

    }

    public ConsumerGroupVo(String consumerGroupName, ConsumerGroupTopicVo topicVo) {
        meta = new ConsumerGroupMetaVo();
        meta.setName(consumerGroupName);
        if (topicVo == null || Util.isEmpty(topicVo.getName()) || Util.isEmpty(consumerGroupName)) {
            throw new IllegalArgumentException("参数不对,consumerGroupName,topicName 不能为空！");
        }
        topics = new ConcurrentHashMap<>();
        topics.put(topicVo.getName(), topicVo);
    }

    public ConsumerGroupVo(String consumerGroupName) {
        meta = new ConsumerGroupMetaVo();
        meta.setName(consumerGroupName);
    }


    public void setGroupName(String name) {
        if (meta == null) {
            if (Util.isEmpty(name)) {
                throw new IllegalArgumentException("参数不对,consumerGroupName不能为空！");
            }
            meta = new ConsumerGroupMetaVo();
            meta.setName(name);
        }
    }

    public boolean addTopic(ConsumerGroupTopicVo topicVo) {
        if (topics == null) {
            topics = new ConcurrentHashMap<>();
        }
        if (topicVo == null || Util.isEmpty(topicVo.getName())) {
            throw new IllegalArgumentException("参数不对,consumerGroupName,topicName不能为空！");
        }
        if (topics.containsKey(topicVo.getName())) {
            return false;
        } else {
            topics.put(topicVo.getName(), topicVo);
            return true;
        }
    }


    public ConsumerGroupMetaVo getMeta() {
        return meta;
    }

    public Map<String, ConsumerGroupTopicVo> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, ConsumerGroupTopicVo> topics) {
        this.topics = topics;
    }

}
