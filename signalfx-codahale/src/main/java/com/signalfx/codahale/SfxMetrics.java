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

    public SfxMetrics(MetricRegistry metricRegistry, MetricMetadata metricMetadata) {
        this.metricRegistry = metricRegistry;
        this.metricMetadata = metricMetadata;
    }

    public Counter counter(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.counter(metricName);
        }
        return build(MetricBuilder.COUNTERS, metricName, dimensions);
    }

    public Counter counter(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.counter(metricName);
        }
        return build(MetricBuilder.COUNTERS, metricName, dimensions);
    }

    public IncrementalCounter incrementalCounter(String metricName, String... dimensions) {
        MetricMetadata.BuilderTagger<IncrementalCounter> metric = metricMetadata
                .forBuilder(IncrementalCounter.Builder.INSTANCE);
        return getT(metricName, metric, dimensions);
    }

    public IncrementalCounter incrementalCounter(String metricName,
                                                 Map<String, String> dimensions) {
        MetricMetadata.BuilderTagger<IncrementalCounter> metric = metricMetadata
                .forBuilder(IncrementalCounter.Builder.INSTANCE);
        return getT(metricName, metric, dimensions);
    }

    public Histogram histogram(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.histogram(metricName);
        }
        return build(MetricBuilder.HISTOGRAMS, metricName, dimensions);
    }

    public Histogram histogram(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.histogram(metricName);
        }
        return build(MetricBuilder.HISTOGRAMS, metricName, dimensions);
    }

    public Timer timer(String metricName, String... dimensions) {
        if (dimensions.length == 0) {
            return metricRegistry.timer(metricName);
        }
        return build(MetricBuilder.TIMERS, metricName, dimensions);
    }

    public Timer timer(String metricName, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return metricRegistry.timer(metricName);
        }
        return build(MetricBuilder.TIMERS, metricName, dimensions);
    }

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

    public SettableLongGauge longGauge(String metricName, String... dimensions) {
        return build(SettableLongGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public SettableLongGauge longGauge(String metricName, Map<String, String> dimensions) {
        return build(SettableLongGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public SettableDoubleGauge doubleGauge(String metricName, String... dimensions) {
        return build(SettableDoubleGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public SettableDoubleGauge doubleGauge(String metricName, Map<String, String> dimensions) {
        return build(SettableDoubleGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public void registerGaugeAsCumulativeCounter(String name, Gauge<?> gauge, String... dimensions) {
        MetricMetadata.Tagger<? extends Gauge<?>> tagger = metricMetadata
                .forMetric(metricRegistry.register(name, gauge))
                .withMetricName(name)
                .withMetricType(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER);
        for (int i = 0; i < dimensions.length - 1; i += 2) {
            tagger.withDimension(dimensions[i], dimensions[i + 1]);
        }
    }

    public void registerGaugeAsCumulativeCounter(String name, Gauge<?> gauge, Map<String, String> dimensions) {
        MetricMetadata.Tagger<? extends Gauge<?>> tagger = metricMetadata
                .forMetric(metricRegistry.register(name, gauge))
                .withMetricName(name)
                .withMetricType(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER);
        if (dimensions != null) {
            for (Map.Entry<String, String> entry : dimensions.entrySet()) {
                tagger.withDimension(entry.getKey(), entry.getValue());
            }
        }
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

    public <T extends Gauge<?>> T registerGauge(String metricName, T metric, String... dimensions) {
        return build(builderForMetric(metric), metricName, dimensions);
    }

    public <T extends Gauge<?>> T registerGauge(String metricName, T metric,
                                                Map<String, String> dimensions) {
        return build(builderForMetric(metric), metricName, dimensions);
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

    public <T extends Metric> boolean unregister(T metric) {
        return metricMetadata.removeMetric(metric, metricRegistry);
    }

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

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public MetricMetadata getMetricMetadata() {
        return metricMetadata;
    }
}
