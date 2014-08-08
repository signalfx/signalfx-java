package com.signalfuse.metrics.metric;

/**
 * An absolute counter is a metric who's only value has meaning dependent upon it's last value. An
 * example would be SNMP counters.
 * 
 * @author jack
 */
public interface CumulativeCounter extends SettableMetric {}
