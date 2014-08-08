package com.signalfuse.metrics.metric.periodic;

/**
 * A gauge who's value is updated every X units of time, set inside your metric factory's creation method.
 * @author jack
 */
public interface PeriodicGauge {
    /**
     * Stop this gauge from ever executing again.
     */
    void cancel();
}
