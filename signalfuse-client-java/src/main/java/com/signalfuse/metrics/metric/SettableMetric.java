package com.signalfuse.metrics.metric;

/**
 * A settable metric is a metric who's value can be set absolutely rather than relative to its last
 * value.
 * 
 * @author jack
 */
public interface SettableMetric extends Metric {
    /**
     * Set the value of this metric to value with long precision.
     * 
     * @param value
     *            New metric value
     */
    void value(long value);

    /**
     * Set the value of this metric to value with double precision.
     * 
     * @param value
     *            New metric value
     */
    void value(double value);
}
