package com.signalfx.codahale.reporter;

import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.MetricName;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Utility functions that make common SignalFx operations easier to do.
 */
public class SfUtil {

    /**
     * <p>
     * Creates a {@link com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType#CUMULATIVE_COUNTER}
     * type metric who's value is returned from a callback.  The metric is internally stored as the
     * Gauge type inside the MetricsRegistry,
     * but the callback is expected to behave like a cumulative counter and the value is sent to
     * SignalFx as a cumulative counter.
     * </p>
     * <p>
     *     This is useful when you can query for an absolute number of events, but cannot register
     *     a callback per event.  Rather than behaving like a Gauge, it will signal a rate of events
     *     to SignalFx.
     * </p>
     * @param metricRegistry    Where the counter lives
     * @param name              Name of the counter
     * @param metricMetadata    Where your metric metadata is tagged
     * @param callback          The callback that gets the counter's current value
     * @return The registered metric
     */
    public static Metric cumulativeCounter(MetricsRegistry metricRegistry,
                                           MetricName name,
                                           MetricMetadata metricMetadata,
                                           Gauge<Long> callback) {
        return metricMetadata.forMetric(metricRegistry.newGauge(name, callback)).withMetricType(
                SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER).metric();
    }

    
}
