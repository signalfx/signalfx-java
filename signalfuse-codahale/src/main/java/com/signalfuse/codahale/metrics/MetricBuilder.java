package com.signalfuse.codahale.metrics;

import com.codahale.metrics.Metric;

/**
 * A copy of the MetricBuilder defined inside {@link com.codahale.metrics.MetricRegistry}, but a
 * public version.
 * @param <T>    Which metric type this builds
 */
public interface MetricBuilder<T extends Metric> {
    public T newMetric();

    public boolean isInstance(Metric metric);
}
