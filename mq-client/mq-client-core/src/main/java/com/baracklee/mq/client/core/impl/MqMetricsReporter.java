package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.MqContext;
import com.codahale.metrics.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MqMetricsReporter extends ScheduledReporter {
    private final Map<String,String> tags;

    private Map<String, AtomicLong> lastReport= new ConcurrentHashMap<>();
    private MqContext mqContext;
    private HttpClient httpClient=null;

    public MqMetricsReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit,
                            TimeUnit durationUnit, Map<String, String> tags, MqContext mqContext) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.tags = tags;
        this.mqContext = mqContext;
        this.httpClient = new HttpClient(60*1000, 60*1000);
    }



    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> sortedMap2, SortedMap<String, Meter> sortedMap3, SortedMap<String, Timer> sortedMap4) {
        if(Util.isEmpty(mqContext.getMetricUrl())) return;
        final long timeStamp=System.currentTimeMillis();
        final Set<Metric> metrics = new HashSet<>();

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            metrics.addAll(buildGauge(entry.getKey(), entry.getValue(), timestamp, tags));
        }
        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            metrics.addAll(buildCounter(entry.getKey(), entry.getValue(), timestamp, tags));
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            metrics.addAll(buildHistograms(entry.getKey(), entry.getValue(), timestamp, tags));
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            metrics.addAll(buildMeters(entry.getKey(), entry.getValue(), timestamp, tags));
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            metrics.addAll(buildTimers(entry.getKey(), entry.getValue(), timestamp, tags));
        }
        try {
            if (!Util.isEmpty(mqContext.getMetricUrl()) && metrics.size() > 0) {
                httpClient.post(mqContext.getMetricUrl(), metrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
