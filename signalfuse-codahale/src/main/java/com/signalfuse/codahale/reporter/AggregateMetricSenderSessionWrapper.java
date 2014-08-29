package com.signalfuse.codahale.reporter;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.flush.AggregateMetricSender;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

class AggregateMetricSenderSessionWrapper implements Closeable {
    private final AggregateMetricSender.Session metricSenderSession;
    private final Set<SignalFuseReporter.MetricDetails> detailsToAdd;
    private final MetricMetadata metricMetadata;
    private final String defaultSourceName;

    AggregateMetricSenderSessionWrapper(
            AggregateMetricSender.Session metricSenderSession,
            Set<SignalFuseReporter.MetricDetails> detailsToAdd,
            MetricMetadata metricMetadata,
            String defaultSourceName, Map<Metric, Long> hardCounterValueCache) {
        this.metricSenderSession = metricSenderSession;
        this.detailsToAdd = detailsToAdd;
        this.metricMetadata = metricMetadata;
        this.defaultSourceName = defaultSourceName;
    }

    public void close() {
        try {
            metricSenderSession.close();
        } catch (Exception e) {
            throw new SignalfuseMetricsException("Unable to close session and send metrics", e);
        }
    }

    // These three called from report
    void addTimer(String key, Timer value) {
        addMetered(key, value);
        addSampling(key, value);
    }

    void addHistogram(String baseName,
                      Histogram histogram) {
        addMetric(histogram, baseName,
                Optional.of(SignalFuseReporter.MetricDetails.COUNT),
                SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER, histogram.getCount());
        addSampling(baseName, histogram);
    }

    void addMetered(String baseName, Metered metered) {
        addMetric(metered, baseName,
                SignalFuseReporter.MetricDetails.COUNT,
                SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER, metered.getCount());
        addMetric(metered, baseName,
                SignalFuseReporter.MetricDetails.RATE_15_MIN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, metered.getFifteenMinuteRate());
        addMetric(metered, baseName,
                SignalFuseReporter.MetricDetails.RATE_1_MIN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, metered.getOneMinuteRate());
        addMetric(metered, baseName,
                SignalFuseReporter.MetricDetails.RATE_5_MIN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, metered.getFiveMinuteRate());

        addMetric(metered, baseName,
                SignalFuseReporter.MetricDetails.RATE_MEAN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, metered.getMeanRate());
    }

    private void addSampling(String baseName, Sampling sampling) {
        Metric metric = (Metric)sampling;
        final Snapshot snapshot = sampling.getSnapshot();
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.MEDIAN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.getMedian());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.PERCENT_75,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.get75thPercentile());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.PERCENT_95,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.get95thPercentile());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.PERCENT_98,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.get98thPercentile());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.PERCENT_99,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.get99thPercentile());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.PERCENT_999,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.get999thPercentile());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.MAX,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.getMax());
        addMetric(metric, baseName,
                SignalFuseReporter.MetricDetails.MIN,
                SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.getMin());


        // These are slower to calculate.  Only calculate if we need.
        if (detailsToAdd.contains(SignalFuseReporter.MetricDetails.STD_DEV)) {
            addMetric(metric, baseName,
                    SignalFuseReporter.MetricDetails.STD_DEV,
                    SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.getStdDev());
        }
        if (detailsToAdd.contains(SignalFuseReporter.MetricDetails.MEAN)) {
            addMetric(metric, baseName,
                    SignalFuseReporter.MetricDetails.MEAN,
                    SignalFuseProtocolBuffers.MetricType.GAUGE, snapshot.getMean());
        }
    }

    void addMetric(Metric metric, String codahaleName,
                             SignalFuseProtocolBuffers.MetricType defaultMetricType,
                             Object originalValue) {
        addMetric(metric, codahaleName, Optional.<SignalFuseReporter.MetricDetails>absent(),
                defaultMetricType, originalValue);
    }

    private void addMetric(Metric metric, String codahaleName, SignalFuseReporter.MetricDetails metricDetails,
                          SignalFuseProtocolBuffers.MetricType defaultMetricType,
                          Object originalValue) {
        addMetric(metric, codahaleName, Optional.of(metricDetails),
                defaultMetricType, originalValue);

    }

    void addMetric(Metric metric, String codahaleName,
                   Optional<SignalFuseReporter.MetricDetails> metricDetails,
                   SignalFuseProtocolBuffers.MetricType defaultMetricType, Object originalValue) {
        final Number value;
        if (originalValue instanceof Number) {
            value = (Number) originalValue;
        } else if (originalValue instanceof Boolean) {
            value = ((Boolean)originalValue).booleanValue() ? 1 : 0;
        } else {
            // Unsupported type
            return;
        }
        final String metricDetailsMetricNameSuffix;
        if (metricDetails.isPresent()) {
            if (!detailsToAdd.contains(metricDetails.get())) {
                return;
            }
            metricDetailsMetricNameSuffix = "." + metricDetails.get().getDescription();
        } else {
            metricDetailsMetricNameSuffix = "";
        }
        Optional<SignalFuseProtocolBuffers.MetricType> userSetMetricType = metricMetadata.getMetricType(metric);
        SignalFuseProtocolBuffers.MetricType metricType = userSetMetricType.or(defaultMetricType);
        Map<String, String> tags = metricMetadata.getTags(metric);
        final String sourceName = Optional.fromNullable(tags.get(MetricMetadata.SOURCE)).or(defaultSourceName);
        final String metricName = Optional.fromNullable(tags.get(MetricMetadata.METRIC)).or(codahaleName) + metricDetailsMetricNameSuffix;
        if (value instanceof Long || value instanceof Integer || value instanceof Short) {
            metricSenderSession.setDatapoint(sourceName, metricName, metricType, value.longValue());
        } else {
            final double doubleToSend = value.doubleValue();
            if (Double.isInfinite(doubleToSend) || Double.isNaN(doubleToSend)) {
                return;
            }
            metricSenderSession.setDatapoint(sourceName, metricName, metricType, value.doubleValue());
        }
    }
}
