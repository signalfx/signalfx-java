package com.signalfuse.codahale.reporter;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Utility functions that make common Signalfuse operations easier to do.
 */
public class SfUtil {
    private SfUtil(){}

    /**
     * <p>
     * Creates a {@link com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers.MetricType#CUMULATIVE_COUNTER}
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
        return metricMetadata.tagMetric(metricRegistry.register(name, callback)).withMetricType(
                SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER).metric();
    }
}
