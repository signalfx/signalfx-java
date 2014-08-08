package com.signalfuse.metrics.metric.periodic;

/**
 * Used with a periodic gauge.  Will set the gauge's value to getValue() every X units of time.
 * @author jack
 */
public interface LongCallback {
    /**
     * Override to return a long value that is the <b>current</b> value of the gauge
     * 
     * @return Current value of the gauge
     */
    long getValue();
}
