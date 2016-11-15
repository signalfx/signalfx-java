/**
* Copyright (C) 2015 SignalFx, Inc.
*/
package com.signalfx.codahale.reporter;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import java.util.concurrent.TimeUnit;

class AggregateMetricSenderSessionWrapper implements Closeable {

    private static final ImmutableSet<String> ignoredDimensions = ImmutableSet.of(
            MetricMetadata.SOURCE, MetricMetadata.METRIC);

    private final AggregateMetricSender.Session metricSenderSession;
    private final Set<SignalFxReporter.MetricDetails> detailsToAdd;
    private final MetricMetadata metricMetadata;
    private final String defaultSourceName;
    private final String sourceDimension;
    private final boolean injectCurrentTimestamp;
    private final ImmutableMap<String, DimensionInclusion> defaultDimensions;
    private final TimeUnit reportingTimeUnit;
    private final TimeUnit DEFAULT_REPORTING_UNIT = TimeUnit.NANOSECONDS; //Default Time Unit for metric reporting is nanoseconds

    AggregateMetricSenderSessionWrapper(
            AggregateMetricSender.Session metricSenderSession,
            Set<SignalFxReporter.MetricDetails> detailsToAdd,
            MetricMetadata metricMetadata,
            String defaultSourceName,
            String sourceDimension,
            TimeUnit reportingTimeUnit) {
        this(metricSenderSession, detailsToAdd, metricMetadata, defaultSourceName, sourceDimension,
                false, Collections.<String, DimensionInclusion> emptyMap(), reportingTimeUnit);
    }

    AggregateMetricSenderSessionWrapper(
            AggregateMetricSender.Session metricSenderSession,
            Set<SignalFxReporter.MetricDetails> detailsToAdd,
            MetricMetadata metricMetadata,
            String defaultSourceName,
            String sourceDimension,
            boolean injectCurrentTimestamp,
            Map<String, DimensionInclusion> defaultDimensions,
            TimeUnit reportingTimeUnit) {
        this.metricSenderSession = metricSenderSession;
        this.detailsToAdd = detailsToAdd;
        this.metricMetadata = metricMetadata;
        this.defaultSourceName = defaultSourceName;
        this.sourceDimension = sourceDimension;
        this.injectCurrentTimestamp = injectCurrentTimestamp;
        this.defaultDimensions = ImmutableMap.copyOf(defaultDimensions);
        this.reportingTimeUnit = reportingTimeUnit;
    }

    @Override
    public void close() {
        try {
            metricSenderSession.close();
        } catch (Exception e) {
            throw new SignalFxMetricsException("Unable to close session and send metrics", e);
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
                Optional.of(SignalFxReporter.MetricDetails.COUNT),
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, histogram.getCount());
        addSampling(baseName, histogram);
    }

    void addMetered(String baseName, Metered metered) {
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.COUNT,
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, metered.getCount());
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_15_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)metered.getFifteenMinuteRate(),DEFAULT_REPORTING_UNIT));
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_1_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long) metered.getOneMinuteRate(),DEFAULT_REPORTING_UNIT));
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_5_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)metered.getFiveMinuteRate(),DEFAULT_REPORTING_UNIT));

        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_MEAN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)metered.getMeanRate(),DEFAULT_REPORTING_UNIT));
    }

    private void addSampling(String baseName, Sampling sampling) {
        Metric metric = (Metric)sampling;
        final Snapshot snapshot = sampling.getSnapshot();
        
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MEDIAN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.getMedian(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_75,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.get75thPercentile(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_95,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.get95thPercentile(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_98,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.get98thPercentile(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_99,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.get99thPercentile(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_999,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.get999thPercentile(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MAX,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.getMax(),DEFAULT_REPORTING_UNIT));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.getMin(),DEFAULT_REPORTING_UNIT));


        // These are slower to calculate.  Only calculate if we need.
        if (detailsToAdd.contains(SignalFxReporter.MetricDetails.STD_DEV)) {
            addMetric(metric, baseName,
                    SignalFxReporter.MetricDetails.STD_DEV,
                    SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.getStdDev(),DEFAULT_REPORTING_UNIT));
        }
        if (detailsToAdd.contains(SignalFxReporter.MetricDetails.MEAN)) {
            addMetric(metric, baseName,
                    SignalFxReporter.MetricDetails.MEAN,
                    SignalFxProtocolBuffers.MetricType.GAUGE, reportingTimeUnit.convert((long)snapshot.getMean(),DEFAULT_REPORTING_UNIT));
        }
    }

    void addMetric(Metric metric, String codahaleName,
                             SignalFxProtocolBuffers.MetricType defaultMetricType,
                             Object originalValue) {
        addMetric(metric, codahaleName, Optional.<SignalFxReporter.MetricDetails>absent(),
                defaultMetricType, originalValue);
    }

    private void addMetric(Metric metric, String codahaleName, SignalFxReporter.MetricDetails metricDetails,
                          SignalFxProtocolBuffers.MetricType defaultMetricType,
                          Object originalValue) {
        addMetric(metric, codahaleName, Optional.of(metricDetails),
                defaultMetricType, originalValue);

    }

    void addMetric(Metric metric, String codahaleName,
                   Optional<SignalFxReporter.MetricDetails> metricDetails,
                   SignalFxProtocolBuffers.MetricType defaultMetricType, Object originalValue) {
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
        Optional<SignalFxProtocolBuffers.MetricType> userSetMetricType = metricMetadata.getMetricType(metric);
        SignalFxProtocolBuffers.MetricType metricType = userSetMetricType.or(defaultMetricType);
        Map<String, String> tags = metricMetadata.getTags(metric);
        final String sourceName = Optional.fromNullable(tags.get(MetricMetadata.SOURCE)).or(defaultSourceName);
        final String metricName = Optional.fromNullable(tags.get(MetricMetadata.METRIC)).or(codahaleName) + metricDetailsMetricNameSuffix;

        SignalFxProtocolBuffers.DataPoint.Builder builder = SignalFxProtocolBuffers.DataPoint
                .newBuilder()
                .setMetric(metricName)
                .setMetricType(metricType);

        if (!sourceDimension.equals("") && !tags.containsKey(sourceDimension)) {
            builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                    .setKey(sourceDimension).setValue(sourceName));
        }

        for (Map.Entry<String, String> entry: tags.entrySet()) {
            if (!ignoredDimensions.contains(entry.getKey())) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey(entry.getKey()).setValue(entry.getValue()));
            }
        }

        for (Map.Entry<String, DimensionInclusion> entry : defaultDimensions.entrySet()) {
            String dimName = entry.getKey();
            DimensionInclusion dimValue = entry.getValue();
            if (!ignoredDimensions.contains(dimName) && !tags.containsKey(dimName)
                    && dimValue.shouldInclude(metricType)) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder().setKey(dimName)
                        .setValue(dimValue.getValue()));
            }
        }

        if (injectCurrentTimestamp) {
            final long currentTimestamp = System.currentTimeMillis();
            builder.setTimestamp(currentTimestamp);
        }

        if (value instanceof Long || value instanceof Integer || value instanceof Short) {
            builder.setValue(SignalFxProtocolBuffers.Datum
                    .newBuilder().setIntValue(value.longValue()));
        } else {
            final double doubleToSend = value.doubleValue();
            if (Double.isInfinite(doubleToSend) || Double.isNaN(doubleToSend)) {
                return;
            }
            builder.setValue(SignalFxProtocolBuffers.Datum.newBuilder()
                    .setDoubleValue(doubleToSend));
        }

        metricSenderSession.setDatapoint(builder.build());
    }
}
