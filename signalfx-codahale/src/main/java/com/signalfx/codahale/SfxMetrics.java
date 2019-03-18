/**
 * Copyright (C) 2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.codahale;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Meter;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.metrics.MetricBuilder;
import com.signalfx.codahale.metrics.SettableDoubleGauge;
import com.signalfx.codahale.metrics.SettableLongGauge;
import com.signalfx.codahale.reporter.IncrementalCounter;
import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * A utility class for declaring Codahale metrics with additional dimensions.
 *
 * @author max
 */
public class SfxMetrics {

    private final MetricRegistry metricRegistry;
    private final MetricMetadata metricMetadata;

    /**
     * Create a new instance of this class backed by the given metric registry.
     *
     * @param metricRegistry
     *         The Codahale {@link MetricRegistry}.
     * @param metricMetadata
     *         The SignalFx {@link MetricMetadata} registry.
     */
    public SfxMetrics(MetricRegistry metricRegistry, MetricMetadata metricMetadata) {
        this.metricRegistry = metricRegistry;
        this.metricMetadata = metricMetadata;
    }


    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public MetricMetadata getMetricMetadata() {
        return metricMetadata;
    }

    /**
     * Get or create a new counter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link Counter} instance.
     */
    public Counter counter(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.counter(metricName);
        }
        return build(MetricBuilder.COUNTERS, metricName, dimensions);
    }

    /**
     * Get or create a new counter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link Counter} instance.
     */
    public Counter counter(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.counter(metricName);
        }
        return build(MetricBuilder.COUNTERS, metricName, dimensions);
    }

    /**
     * Get or create a new meter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link Meter} instance.
     */
    public Meter meter(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.meter(metricName);
        }
        return build(MetricBuilder.METERS, metricName, dimensions);
    }

    /**
     * Get or create a new meter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link Meter} instance.
     */
    public Meter meter(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.meter(metricName);
        }
        return build(MetricBuilder.METERS, metricName, dimensions);
    }

    /**
     * Get or create a new incremental counter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link IncrementalCounter} instance.
     */
    public IncrementalCounter incrementalCounter(String metricName, String... dimensions) {
        MetricMetadata.BuilderTagger<IncrementalCounter> metric = metricMetadata
                .forBuilder(IncrementalCounter.Builder.INSTANCE);
        return getT(metricName, metric, dimensions);
    }

    /**
     * Get or create a new incremental counter.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link IncrementalCounter} instance.
     */
    public IncrementalCounter incrementalCounter(String metricName,
                                                 Map<String, String> dimensions) {
        MetricMetadata.BuilderTagger<IncrementalCounter> metric = metricMetadata
                .forBuilder(IncrementalCounter.Builder.INSTANCE);
        return getT(metricName, metric, dimensions);
    }

    /**
     * Get or create a new histogram.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link Histogram} instance.
     */
    public Histogram histogram(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.histogram(metricName);
        }
        return build(MetricBuilder.HISTOGRAMS, metricName, dimensions);
    }

    /**
     * Get or create a new histogram.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link Histogram} instance.
     */
    public Histogram histogram(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.histogram(metricName);
        }
        return build(MetricBuilder.HISTOGRAMS, metricName, dimensions);
    }

    /**
     * Get or create a new timer.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link Timer} instance.
     */
    public Timer timer(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.timer(metricName);
        }
        return build(MetricBuilder.TIMERS, metricName, dimensions);
    }

    /**
     * Get or create a new timer.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link Timer} instance.
     */
    public Timer timer(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.timer(metricName);
        }
        return build(MetricBuilder.TIMERS, metricName, dimensions);
    }

    /**
     * Track the execution of the given {@link Callable} with success, failure and timer metrics.
     *
     * @param function
     *         The {@link Callable} to execute.
     * @param metricPrefix
     *         A prefix for the metric names. Successes are counted by a "prefix.success" metric;
     *         failures by a "prefix.failure" metric, and the {@link Callable}'s execution is
     *         tracked by a "prefix.time" timer.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @param <T>
     *         The return type of the {@link Callable}.
     * @return The return value of the {@link Callable}.
     */
    public <T> T track(Callable<T> function, String metricPrefix, String... dimensions) {
        long startTime = System.currentTimeMillis();
        try {
            T result = function.call();
            counter(metricPrefix + ".success", dimensions).inc();
            return result;
        } catch (Exception ex) {
            counter(metricPrefix + ".failure", dimensions).inc();
            Throwables.throwIfUnchecked(ex);
            throw new RuntimeException(ex);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            timer(metricPrefix + ".time", dimensions).update(duration, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Execute, with retries and tracking, the execution of the given {@link Callable}.
     *
     * @param function
     *         The {@link Callable} to execute.
     * @param maxRetries
     *         The maximum number of retries of the execution.
     * @param delay
     *         How long to wait between retries.
     * @param unit
     *         The unit of the retry delay.
     * @param metricPrefix
     *         A prefix for the metric names. Successes are counted by a "prefix.success" metric;
     *         failures by a "prefix.failure" metric, and the {@link Callable}'s execution is
     *         tracked by a "prefix.time" timer.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @param <T>
     *         The return type of the {@link Callable}.
     * @return The return value of the {@link Callable}.
     */
    public <T> T trackWithRetries(Callable<T> function, int maxRetries, long delay, TimeUnit unit,
                                  String metricPrefix, String... dimensions) {
        int retryCounter = 0;
        while (true) {
            try {
                return track(function, metricPrefix, dimensions);
            } catch (RuntimeException ex) {
                counter(metricPrefix + ".retries", dimensions).inc();
                retryCounter++;
                if (retryCounter > maxRetries) {
                    counter(metricPrefix + ".maxRetriesReached", dimensions).inc();
                    throw ex;
                }
                try {
                    unit.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Get or create a new settable gauge metric for long integer values.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link SettableLongGauge} instance.
     */
    public SettableLongGauge longGauge(String metricName, String... dimensions) {
        return build(SettableLongGauge.Builder.INSTANCE, metricName, dimensions);
    }

    /**
     * Get or create a new settable gauge metric for long integer values.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link SettableLongGauge} instance.
     */
    public SettableLongGauge longGauge(String metricName, Map<String, String> dimensions) {
        return build(SettableLongGauge.Builder.INSTANCE, metricName, dimensions);
    }

    /**
     * Get or create a new settable gauge metric for double precision floating point values.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @return The {@link SettableDoubleGauge} instance.
     */
    public SettableDoubleGauge doubleGauge(String metricName, String... dimensions) {
        return build(SettableDoubleGauge.Builder.INSTANCE, metricName, dimensions);
    }

    /**
     * Get or create a new settable gauge metric for double precision floating point values.
     *
     * @param metricName
     *         The metric name.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     * @return The {@link SettableDoubleGauge} instance.
     */
    public SettableDoubleGauge doubleGauge(String metricName, Map<String, String> dimensions) {
        return build(SettableDoubleGauge.Builder.INSTANCE, metricName, dimensions);
    }

    /**
     * Register the given {@link Gauge} as a cumulative counter.
     *
     * Cumulative counters fundamentally function like gauges, but use a delta rollup by default.
     * This method allows you to report a gauge that measures a monotonically increasing value as a
     * cumulative counter to SignalFx.
     *
     * @param metricName
     *         The metric name.
     * @param gauge
     *         The {@link Gauge} instance.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     */
    public void registerGaugeAsCumulativeCounter(String metricName, Gauge<?> gauge,
                                                 String... dimensions) {
        MetricMetadata.Tagger<? extends Gauge<?>> tagger = metricMetadata
                .forMetric(metricRegistry.register(metricName, gauge))
                .withMetricName(metricName)
                .withMetricType(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER);
        for (int i = 0; i < dimensions.length - 1; i += 2) {
            tagger.withDimension(dimensions[i], dimensions[i + 1]);
        }
    }

    /**
     * Register the given {@link Gauge} as a cumulative counter.
     *
     * Cumulative counters fundamentally function like gauges, but use a delta rollup by default.
     * This method allows you to report a gauge that measures a monotonically increasing value as a
     * cumulative counter to SignalFx.
     *
     * @param metricName
     *         The metric name.
     * @param gauge
     *         The {@link Gauge} instance.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map.
     */
    public void registerGaugeAsCumulativeCounter(String metricName, Gauge<?> gauge,
                                                 Map<String, String> dimensions) {
        MetricMetadata.Tagger<? extends Gauge<?>> tagger = metricMetadata
                .forMetric(metricRegistry.register(metricName, gauge))
                .withMetricName(metricName)
                .withMetricType(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER);
        if (dimensions != null) {
            for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                tagger.withDimension(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Register an existing {@link Gauge} instance to start reporting it.
     *
     * @param metricName
     *         The metric name.
     * @param gauge
     *         The {@link Gauge} instance.
     * @param dimensions
     *         Additional dimension key/value pairs (an even number of strings must be provided).
     * @param <T>
     *         The concrete type of the gauge instance.
     * @return The gauge instance itself.
     */
    public <T extends Gauge<?>> T registerGauge(String metricName, T gauge, String... dimensions) {
        return build(builderForMetric(gauge), metricName, dimensions);
    }

    /**
     * Register an existing {@link Gauge} instance to start reporting it.
     *
     * @param metricName
     *         The metric name.
     * @param gauge
     *         The {@link Gauge} instance.
     * @param dimensions
     *         Additional dimension key/value pairs, as a map
     * @param <T>
     *         The concrete type of the gauge instance.
     * @return The gauge instance itself.
     */
    public <T extends Gauge<?>> T registerGauge(String metricName, T gauge,
                                                Map<String, String> dimensions) {
        return build(builderForMetric(gauge), metricName, dimensions);
    }

    /**
     * Unregister the given metric to stop reporting it.
     *
     * @param metric
     *         The metric instance.
     * @param <T>
     *         The concrete type of the metric object.
     * @return True iff the metric was previously registered and was removed; false otherwise.
     */
    public <T extends Metric> boolean unregister(T metric) {
        return metricMetadata.removeMetric(metric, metricRegistry);
    }

    /**
     * Unregister a set of metrics at once.
     *
     * @param metricsToRemove
     *         An array of metric instances to stop reporting.
     * @return The number of metrics that were actually unregistered.
     */
    public int unregister(Metric... metricsToRemove) {
        final Set<Metric> toRemove = ImmutableSet.copyOf(metricsToRemove);
        final AtomicInteger totalRemoved = new AtomicInteger(0);
        metricRegistry.removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                final boolean shouldRemove = toRemove.contains(metric);
                if (shouldRemove) {
                    totalRemoved.incrementAndGet();
                }
                return shouldRemove;
            }
        });
        return totalRemoved.get();
    }

    private <T extends Gauge<?>> MetricBuilder<T> builderForMetric(final T metric) {
        return new MetricBuilder<T>() {
            @Override
            public T newMetric() {
                return metric;
            }

            @Override
            public boolean isInstance(Metric otherMetric) {
                return otherMetric.getClass().isInstance(metric);
            }
        };
    }

    private <T extends Metric> T build(MetricBuilder<T> builder, String metricName,
                                       String... dimensions) {
        MetricMetadata.BuilderTagger<T> tagger = metricMetadata.forBuilder(builder);
        return getT(metricName, tagger, dimensions);
    }

    private <T extends Metric> T build(MetricBuilder<T> builder, String metricName,
                                       Map<String, String> dimensions) {
        MetricMetadata.BuilderTagger<T> tagger = metricMetadata.forBuilder(builder);
        return getT(metricName, tagger, dimensions);
    }

    private <T extends Metric> T getT(String metricName, MetricMetadata.BuilderTagger<T> tagger,
                                      String[] dimensions) {
        Preconditions.checkArgument(dimensions.length % 2 == 0,
                "Dimensions parameter should have even number of elements");
        tagger.withMetricName(metricName);
        for (int i = 0; i < dimensions.length - 1; i += 2) {
            tagger.withDimension(dimensions[i], dimensions[i + 1]);
        }
        return tagger.createOrGet(metricRegistry);
    }

    private <T extends Metric> T getT(String metricName, MetricMetadata.BuilderTagger<T> tagger,
                                      Map<String, String> dimensions) {
        tagger.withMetricName(metricName);
        if (dimensions != null) {
            for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                tagger.withDimension(entry.getKey(), entry.getValue());
            }
        }
        return tagger.createOrGet(metricRegistry);
    }
}
