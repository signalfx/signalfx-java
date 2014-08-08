package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.metric.Metric;

/**
 * A metric that tracks its value internally to the metric class.
 * 
 * @author jack
 */
public interface TrackedMetric extends Metric {
    /**
     * Returns the value tracked inside this metric. Null if the metric has no current value. <br />
     * 
     * This method is used internally by the library to optimize signalboost calls. Do <b>NOT</b>
     * call this externally as a client of the library.
     * 
     * @return
     */
    Number clearAndGetCurrentNumber();
}
