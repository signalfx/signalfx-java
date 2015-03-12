package com.signalfuse.codahale.reporter;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.signalfuse.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Utility functions that make common Signalfuse operations easier to do.
 */
public class SfUtil {
    private SfUtil(){}

    /**
     * <p>
     * Creates a {@link com.signalfuse.metrics.protobuf.SignalFxProtocolBuffers.MetricType#CUMULATIVE_COUNTER}
     * type metric who's value is returned from a callback.  The metric is internally stored as the
     * {@link com.codahale.metrics.Gauge} type inside the {@link com.codahale.metrics.MetricRegistry},
     * but the callback is expected to behave like a cumulative counter and the value is sent to
     * Signalfuse as a cumulative counter.
     * </p>
     * <p>
     *     This is useful when you can query for an absolute number of events, but cannot register
     *     a callback per event.  Rather than behaving like a Gauge, it will signal a rate of events
     *     to Signalfuse.
     * </p>
     * @param metricRegistry    Where the counter lives
     * @param name              Name of the counter
     * @param metricMetadata    Where your metric metadata is tagged
     * @param callback          The callback that gets the counter's current value
     * @return The registered metric
     */
    public static Metric cumulativeCounter(MetricRegistry metricRegistry,
                                           String name,
                                           MetricMetadata metricMetadata,
                                           Gauge<Long> callback) {
        return metricMetadata.forMetric(metricRegistry.register(name, callback)).withMetricType(
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER).metric();
    }

    /**
     * Removes any of the given metrics from the registry and returns the number of metrics removed
     * @param metricRegistry     Registry to remove from
     * @param metricsToRemove    Which metrics to remove
     * @return The number of metrics removed
     */
    public static int removeMetrics(MetricRegistry metricRegistry, final Metric... metricsToRemove) {
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
}
