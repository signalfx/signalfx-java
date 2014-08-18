package com.signalfuse.codahale.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.google.common.util.concurrent.AtomicDouble;

/**
 * Same as {@link SettableLongGauge} but for a double
 */
public class SettableDoubleGauge implements Metric, Gauge<Double> {
    private final AtomicDouble value = new AtomicDouble();
    public void setValue(double value) {
        this.value.set(value);
    }
    public Double getValue() {
        return value.get();
    }
}
