package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.metric.Metric;
import com.baracklee.mq.biz.common.metric.TagName;
import com.baracklee.mq.biz.common.metric.TagNameUtil;
import com.baracklee.mq.biz.common.util.Util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetricsCollector {
    private final String name;
    private Map<String,String> tags;

    private final long timestamp;

    private final Set<Metric> metrics = new HashSet<Metric>();

    private MetricsCollector(String name, Map<String, String> tags, long timestamp) {
        TagName tagName = TagNameUtil.parse(name);
        this.name = tagName.getName();
        this.tags = tagName.getTags();
        if(tags!=null&&tags.size()>0){
            this.tags.putAll(tags);
        }
        if(this.tags!=null&&this.tags.size()==0){
            this.tags=null;
        }
        this.timestamp = timestamp;
    }

    public static MetricsCollector createNew(String name, Map<String, String> tags, long timestamp) {
        return new MetricsCollector(name, tags, timestamp);
    }

    public MetricsCollector addMetric(String metricName, Object value) {
        String name = Util.isEmpty(metricName) ? this.name : this.name + "." + metricName;
        this.metrics.add(new Metric(name, tags, timestamp, value));
        return this;
    }

    public Set<Metric> build() {
        return metrics;
    }


}
