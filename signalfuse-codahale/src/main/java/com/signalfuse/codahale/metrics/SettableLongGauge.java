package com.signalfuse.codahale.metrics;

import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

/**
 * Works like a Gauge, but rather than getting its value from a callback, the value
 * is set when needed.  This can be somewhat convienent, but direct use of a Gauge is likely better
 */
public class SettableLongGauge implements Metric, Gauge<Long> {
    private final AtomicLong value = new AtomicLong();
    public void setValue(long value) {
        this.value.set(value);
    }
    public Long getValue() {
        return value.get();
    }
}
