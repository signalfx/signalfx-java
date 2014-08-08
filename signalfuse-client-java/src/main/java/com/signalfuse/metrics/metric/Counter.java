package com.signalfuse.metrics.metric;

/**
 * A counter is a metric that is changed relative to it's last value without knowing or caring what
 * the last value was.
 * 
 * @author jack
 */
public interface Counter extends IncrementableMetric {}
