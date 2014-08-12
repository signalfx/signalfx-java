package com.signalfuse.codahale.metrics;

import java.io.Closeable;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.flush.AggregateMetricSender;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * Reporter object for codahale metrics that reports values to com.signalfuse.signalfuse at some interval.
 */
public class SignalFuseReporter extends ScheduledReporter {
    private final AggregateMetricSender aggregateMetricSender;
    private final Set<MetricDetails> detailsToAdd;

    /**
     * Creates a new {@link com.codahale.metrics.ScheduledReporter} instance.
     *
     * @param registry
     *            the {@link com.codahale.metrics.MetricRegistry} containing the metrics this
     *            reporter will report
     * @param name
     *            the reporter's name
     * @param filter          Which metrics to not report
     * @param detailsToAdd    Which types of metric details to report
     */
    protected SignalFuseReporter(MetricRegistry registry, String name, MetricFilter filter,
                                 TimeUnit rateUnit, TimeUnit durationUnit,
                                 AggregateMetricSender aggregateMetricSender, Set<MetricDetails> detailsToAdd) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.aggregateMetricSender = aggregateMetricSender;
        this.detailsToAdd = detailsToAdd;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        AggregateMetricSenderSessionWrapper session = new AggregateMetricSenderSessionWrapper(aggregateMetricSender.createSession());
        try {
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                Object gaugeValue = entry.getValue().getValue();
                if (gaugeValue instanceof Number) {
                    session.reportGauge(entry.getKey(), (Number) gaugeValue);
                }
            }
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                session.metricSenderSession.setCumulativeCounter(entry.getKey(), entry.getValue().getCount());
            }
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                session.addHistogram(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                session.addMetered(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                session.addTimer(entry.getKey(), entry.getValue());
            }
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                // Unable to register... these exceptions handled by AggregateMetricSender
            }
        }
    }
    
    private final class AggregateMetricSenderSessionWrapper implements Closeable {
        private final AggregateMetricSender.Session metricSenderSession;

        private AggregateMetricSenderSessionWrapper(AggregateMetricSender.Session metricSenderSession) {
            this.metricSenderSession = metricSenderSession;
        }

        public void close() {
            try {
                metricSenderSession.close();
            } catch (Exception e) {
                throw new SignalfuseMetricsException("Unable to close session and send metrics", e);
            }
        }

        // These three called from report
        private void addTimer(String key, Timer value) {
            addMetered(key, value);
            addSampling(key, value);
        }

        private void addHistogram(String baseName,
                                  Histogram histogram) {
            addCounting(baseName, histogram);
            addSampling(baseName, histogram);
        }

        private void addMetered(String baseName, Metered metered) {
            addCounting(baseName, metered);
            checkedAdd(MetricDetails.RATE_15_MIN, baseName, metered.getFifteenMinuteRate());
            checkedAdd(MetricDetails.RATE_5_MIN, baseName, metered.getFiveMinuteRate());
            checkedAdd(MetricDetails.RATE_1_MIN, baseName, metered.getOneMinuteRate());
            if (detailsToAdd.contains(MetricDetails.RATE_MEAN)) {
                checkedAdd(MetricDetails.RATE_MEAN, baseName, metered.getMeanRate());
            }
        }

        // Shared
        private void addCounting(String baseName, Counting counting) {
            checkedAddCumulativeCounter(MetricDetails.COUNT, baseName, counting.getCount());
        }

        private void addSampling(String baseName, Sampling sampling) {
            final Snapshot snapshot = sampling.getSnapshot();
            checkedAdd(MetricDetails.MEDIAN, baseName, snapshot.getMedian());
            checkedAdd(MetricDetails.PERCENT_75, baseName, snapshot.get75thPercentile());
            checkedAdd(MetricDetails.PERCENT_95, baseName, snapshot.get95thPercentile());
            checkedAdd(MetricDetails.PERCENT_98, baseName, snapshot.get98thPercentile());
            checkedAdd(MetricDetails.PERCENT_99, baseName, snapshot.get99thPercentile());
            checkedAdd(MetricDetails.PERCENT_999, baseName, snapshot.get999thPercentile());
            checkedAdd(MetricDetails.MAX, baseName, snapshot.getMax());
            checkedAdd(MetricDetails.MIN, baseName, snapshot.getMin());

            // These are slower to calculate.  Only calculate if we need.
            if (detailsToAdd.contains(MetricDetails.STD_DEV)) {
                checkedAdd(MetricDetails.STD_DEV, baseName, snapshot.getStdDev());
            }
            if (detailsToAdd.contains(MetricDetails.MEAN)) {
                checkedAdd(MetricDetails.MEAN, baseName, snapshot.getMean());
            }
        }

        private void reportGauge(String baseName, Number value) {
            if (Double.isInfinite(value.doubleValue()) || Double.isNaN(value.doubleValue())) {
                return;
            }
            if (value instanceof Long || value instanceof Integer) {
                metricSenderSession.setGauge(baseName, value.longValue());
            } else {
                metricSenderSession.setGauge(baseName, value.doubleValue());
            }
        }

        // helpers
        private void checkedAddCumulativeCounter(MetricDetails type, String baseName, long value) {
            if (detailsToAdd.contains(type)) {
                metricSenderSession.setCumulativeCounter(baseName + '.' + type.getDescription(), value);
            }
        }

        private void checkedAdd(MetricDetails type, String baseName, double value) {
            if (detailsToAdd.contains(type)) {
                metricSenderSession.setGauge(baseName + '.' + type.getDescription(), value);
            }
        }

        private void checkedAdd(MetricDetails type, String baseName, long value) {
            if (detailsToAdd.contains(type)) {
                metricSenderSession.setGauge(baseName + '.' + type.getDescription(), value);
            }
        }
    }

    public enum MetricDetails {
        // For {@link com.codahale.metrics.Sampling}
        MEDIAN("median"),
        PERCENT_75("75th"),
        PERCENT_95("95th"),
        PERCENT_98("98th"),
        PERCENT_99("99th"),
        PERCENT_999("999th"),
        MAX("max"),
        MIN("min"),
        STD_DEV("stddev"),
        MEAN("mean"),

        // For {@link com.codahale.metrics.Counting}
        COUNT("count"),

        // For {@link com.codahale.metrics.Metered}
        RATE_MEAN("rate.mean"),
        RATE_1_MIN("rate.1min"),
        RATE_5_MIN("rate.5min"),
        RATE_15_MIN("rate.15min");
        public static final Set<MetricDetails> ALL = EnumSet.allOf(MetricDetails.class);
        private final String description;

        MetricDetails(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
