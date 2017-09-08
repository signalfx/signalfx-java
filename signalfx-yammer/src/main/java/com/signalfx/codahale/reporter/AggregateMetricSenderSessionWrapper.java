package com.signalfx.codahale.reporter;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.stats.Snapshot;

/**
 *
 * class to Aggregate and Send metrics
 * used by SignalFxReporter in report() method
 *
 */

class AggregateMetricSenderSessionWrapper implements Closeable {
    private final AggregateMetricSender.Session metricSenderSession;
    private final Set<SignalFxReporter.MetricDetails> detailsToAdd;
    private final MetricMetadata metricMetadata;
    private final String defaultSourceName;
    private final String sourceDimension;
    private final boolean injectCurrentTimestamp;
    private final boolean sendExtraMetricDimensions;
    private final ImmutableMap<String, String> defaultDimensions;

    AggregateMetricSenderSessionWrapper(
            AggregateMetricSender.Session metricSenderSession,
            Set<SignalFxReporter.MetricDetails> detailsToAdd,
            MetricMetadata metricMetadata,
            String defaultSourceName,
            String sourceDimension,
            boolean injectCurrentTimestamp,
            boolean sendExtraMetricDimensions,
            ImmutableMap<String, String> defaultDimensions) {
        this.metricSenderSession = metricSenderSession;
        this.detailsToAdd = detailsToAdd;
        this.metricMetadata = metricMetadata;
        this.defaultSourceName = defaultSourceName;
        this.sourceDimension = sourceDimension;
        this.injectCurrentTimestamp = injectCurrentTimestamp;
        this.sendExtraMetricDimensions = sendExtraMetricDimensions;
        this.defaultDimensions = defaultDimensions;
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
    void addTimer(MetricName key, Timer value) {
        addMetered(key, value);
        addSampling(key, value);
    }

    /**
     * Add Histogram metric
     * @param baseName
     * @param histogram
     */

    void addHistogram(MetricName baseName,
                      Histogram histogram) {
        addMetric(histogram, baseName,
                Optional.of(SignalFxReporter.MetricDetails.COUNT),
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, histogram.count());
        addSampling(baseName, histogram);
    }

    /**
     * Add Metered metric
     * @param baseName
     * @param metered
     */

    void addMetered(MetricName baseName, Metered metered) {
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.COUNT,
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, metered.count());
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_15_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, metered.fifteenMinuteRate());
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_1_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, metered.oneMinuteRate());
        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_5_MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, metered.fiveMinuteRate());

        addMetric(metered, baseName,
                SignalFxReporter.MetricDetails.RATE_MEAN,
                SignalFxProtocolBuffers.MetricType.GAUGE, metered.meanRate());
    }

    /**
     * Add sampling
     * @param baseName
     * @param sampling
     */

    private void addSampling(MetricName baseName, Sampling sampling) {
        Metric metric = (Metric)sampling;
        final Snapshot snapshot = sampling.getSnapshot();
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MEDIAN,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.getMedian());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_75,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.get75thPercentile());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_95,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.get95thPercentile());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_98,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.get98thPercentile());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_99,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.get99thPercentile());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.PERCENT_999,
                SignalFxProtocolBuffers.MetricType.GAUGE, snapshot.get999thPercentile());
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MAX,
                SignalFxProtocolBuffers.MetricType.GAUGE, getMax(snapshot));
        addMetric(metric, baseName,
                SignalFxReporter.MetricDetails.MIN,
                SignalFxProtocolBuffers.MetricType.GAUGE, getMin(snapshot));


        // These are slower to calculate.  Only calculate if we need.
        if (detailsToAdd.contains(SignalFxReporter.MetricDetails.STD_DEV)) {
            addMetric(metric, baseName,
                    SignalFxReporter.MetricDetails.STD_DEV,
                    SignalFxProtocolBuffers.MetricType.GAUGE, getStdDev(snapshot));
        }
        if (detailsToAdd.contains(SignalFxReporter.MetricDetails.MEAN)) {
            addMetric(metric, baseName,
                    SignalFxReporter.MetricDetails.MEAN,
                    SignalFxProtocolBuffers.MetricType.GAUGE, getMean(snapshot));
        }
    }

    /**
     * Add metric
     * @param metric
     * @param codahaleName
     * @param defaultMetricType
     * @param originalValue
     */

    void addMetric(Metric metric, MetricName codahaleName,
                             SignalFxProtocolBuffers.MetricType defaultMetricType,
                             Object originalValue) {
        addMetric(metric, codahaleName, Optional.<SignalFxReporter.MetricDetails>absent(),
                defaultMetricType, originalValue);
    }

    /**
     * Add metric
     * @param metric
     * @param codahaleName
     * @param metricDetails
     * @param defaultMetricType
     * @param originalValue
     */

    private void addMetric(Metric metric, MetricName codahaleName, SignalFxReporter.MetricDetails metricDetails,
                          SignalFxProtocolBuffers.MetricType defaultMetricType,
                          Object originalValue) {
        addMetric(metric, codahaleName, Optional.of(metricDetails),
                defaultMetricType, originalValue);

    }

    /**
     * Add metric
     * @param metric
     * @param codahaleName
     * @param metricDetails
     * @param defaultMetricType
     * @param originalValue
     */

    void addMetric(Metric metric, MetricName codahaleName,
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
        final String metricName = Optional.fromNullable(tags.get(MetricMetadata.METRIC)).or(codahaleName.getName()) + metricDetailsMetricNameSuffix;

        SignalFxProtocolBuffers.DataPoint.Builder builder = SignalFxProtocolBuffers.DataPoint
                .newBuilder()
                .setMetric(metricName)
                .setMetricType(metricType);

        if (!sourceDimension.equals("") && !tags.containsKey(sourceDimension)) {
            builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                    .setKey(sourceDimension).setValue(sourceName));
        }

        ImmutableSet<String> ignoredDimensions = ImmutableSet.of(
                MetricMetadata.SOURCE, MetricMetadata.METRIC);

        for (Map.Entry<String, String> entry: tags.entrySet()) {
            if (!ignoredDimensions.contains(entry.getKey())) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey(entry.getKey()).setValue(entry.getValue()));
            }
        }

        if (sendExtraMetricDimensions) {
            if (!Strings.isNullOrEmpty(codahaleName.getGroup())) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey("metric_group").setValue(codahaleName.getGroup()));
            }
            if (!Strings.isNullOrEmpty(codahaleName.getType())) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder()
                        .setKey("metric_type").setValue(codahaleName.getType()));
            }
        }

        for (Map.Entry<String, String> entry : defaultDimensions.entrySet()) {
            String dimName = entry.getKey();
            String dimValue = entry.getValue();
            if (!ignoredDimensions.contains(dimName) && !tags.containsKey(dimName)) {
                builder.addDimensions(SignalFxProtocolBuffers.Dimension.newBuilder().setKey(dimName)
                        .setValue(dimValue));
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

    //--- Aditional Methods

    /**
     * calculate Max value for snapshot
     * @param shapshot
     * @return
     */

    private double getMax(Snapshot snapshot) {
    	double[] values = snapshot.getValues();
    	if (values.length == 0) {
    		return 0;
    	}
    	return values[values.length - 1];
    }

    /**
     * calculate Min value for snapshot
     * @param shapshot
     * @return
     */

    private double getMin(Snapshot snapshot) {
    	double[] values = snapshot.getValues();
    	if (values.length == 0) {
    		return 0;
    	}
    	return values[0];
    }

    /**
     * calculate standard deviation for snapshot
     * @param shapshot
     * @return
     */

    private double getStdDev(Snapshot snapshot) {
        // two-pass algorithm for variance, avoids numeric overflow
    	double[] values = snapshot.getValues();

        if (values.length <= 1) {
            return 0;
        }

        final double mean = getMean(snapshot);
        double sum = 0;

        for (double value : values) {
            final double diff = value - mean;
            sum += diff * diff;
        }

        final double variance = sum / (values.length - 1);
        return Math.sqrt(variance);
    }

    /**
     * calculate Mean value for snapshot
     * @param shapshot
     * @return
     */

    private double getMean(Snapshot shapshot) {

    	double[] values = shapshot.getValues();

        if (values.length == 0) {
            return 0;
        }

        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

}
