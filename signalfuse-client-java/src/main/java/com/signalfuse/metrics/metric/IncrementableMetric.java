package com.signalfuse.metrics.metric;

/**
 * A metric that accepts increment calls.
 * 
 * @author jack
 */
public interface IncrementableMetric extends Metric {

    /**
     * Same as incr(1)
     */
    void incr();

    void incr(double amount);

    void incr(long amount);

    /**
     * Same as decr(1)
     */
    void decr();

    void decr(double amount);

    void decr(long amount);
}
