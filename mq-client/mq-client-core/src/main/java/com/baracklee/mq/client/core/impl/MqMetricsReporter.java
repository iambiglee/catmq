package com.baracklee.mq.client.core.impl;

import com.baracklee.mq.biz.common.metric.Metric;
import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.MqContext;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.*;
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
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        if(Util.isEmpty(mqContext.getMetricUrl())) return;
        final long timestamp=System.currentTimeMillis();
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
    private Set<Metric> buildTimers(String name, Timer timer, long timestamp, Map<String, String> tags) {
        MetricsCollector collector = MetricsCollector.createNew(name, tags, timestamp);
        final Snapshot snapshot = timer.getSnapshot();
        if (getChangeCount(name, timer.getCount()) == 0) {
            return Collections.emptySet();
        }
        return collector.addMetric("count", timer.getCount())
                // convert rate
                .addMetric("m15", convertRate(timer.getFifteenMinuteRate()))
                .addMetric("m5", convertRate(timer.getFiveMinuteRate()))
                .addMetric("m1", convertRate(timer.getOneMinuteRate()))
                .addMetric("mean_rate", convertRate(timer.getMeanRate()))
                // convert duration
                .addMetric("max", convertDuration(snapshot.getMax()))
                .addMetric("min", convertDuration(snapshot.getMin()))
                .addMetric("mean", convertDuration(snapshot.getMean()))
                .addMetric("stddev", convertDuration(snapshot.getStdDev()))
                .addMetric("median", convertDuration(snapshot.getMedian()))
                .addMetric("p75", convertDuration(snapshot.get75thPercentile()))
                .addMetric("p95", convertDuration(snapshot.get95thPercentile()))
                .addMetric("p98", convertDuration(snapshot.get98thPercentile()))
                .addMetric("p99", convertDuration(snapshot.get99thPercentile()))
                .addMetric("p999", convertDuration(snapshot.get999thPercentile())).build();

    }

    private long getChangeCount(String name, long count) {
        if(!this.lastReport.containsKey(name)){
            this.lastReport.put(name, new AtomicLong(0));
        }
        //this.lastReport.putIfAbsent(name, new AtomicLong(0));
        AtomicLong last = this.lastReport.get(name);
        long lastCount = last.getAndSet(count);
        return count - lastCount;
    }

    private Set<Metric> buildGauge(String name, Gauge gauge, long timestamp, Map<String, String> tags) {
        final MetricsCollector collector = MetricsCollector.createNew(name, tags, timestamp);
        return collector.addMetric("value", gauge.getValue()).build();
    }

    private Set<Metric> buildCounter(String name, Counter counter, long timestamp, Map<String, String> tags) {
        final MetricsCollector collector = MetricsCollector.createNew(name, tags, timestamp);
        long changedCount = getChangeCount(name, counter.getCount());
        if (changedCount == 0) {
            return Collections.emptySet();
        }
        return collector.addMetric("value", changedCount).build();
    }

    private Set<Metric> buildHistograms(String name, Histogram histogram, long timestamp, Map<String, String> tags) {
        final MetricsCollector collector = MetricsCollector.createNew(name, tags, timestamp);
        final Snapshot snapshot = histogram.getSnapshot();
        if (getChangeCount(name, histogram.getCount()) == 0) {
            return Collections.emptySet();
        }
        return collector.addMetric("count", histogram.getCount()).addMetric("max", snapshot.getMax())
                .addMetric("min", snapshot.getMin()).addMetric("mean", snapshot.getMean())
                .addMetric("stddev", snapshot.getStdDev()).addMetric("median", snapshot.getMedian())
                .addMetric("p75", snapshot.get75thPercentile()).addMetric("p95", snapshot.get95thPercentile())
                .addMetric("p98", snapshot.get98thPercentile()).addMetric("p99", snapshot.get99thPercentile())
                .addMetric("p999", snapshot.get999thPercentile()).build();
    }

    private Set<Metric> buildMeters(String name, Meter meter, long timestamp, Map<String, String> tags) {
        final MetricsCollector collector = MetricsCollector.createNew(name, tags, timestamp);
        if (getChangeCount(name, meter.getCount()) == 0) {
            return Collections.emptySet();
        }
        return collector.addMetric("count", meter.getCount())
                // convert rate
                .addMetric("mean_rate", convertRate(meter.getMeanRate()))
                .addMetric("m1", convertRate(meter.getOneMinuteRate()))
                .addMetric("m5", convertRate(meter.getFiveMinuteRate()))
                .addMetric("m15", convertRate(meter.getFifteenMinuteRate())).build();
    }
    }
