package com.signalfuse.metrics.metric.periodic.internal;

import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.periodic.DoubleCallback;

/**
 * Package private class that handles executing and passing in gauge values
 * 
 * @author jack
 */
class DoublePeriodicGaugeExecution extends PeriodicGaugeExecution {
    private final DoubleCallback callback;
    private double lastValue;

    DoublePeriodicGaugeExecution(Gauge gauge, DoubleCallback callback) {
        super(gauge);
        this.callback = callback;
    }

    @Override
    public void run() {
        setLastValue(callback.getValue());
        getGauge().value(callback.getValue());
    }

    public double getLastValue() {
        return lastValue;
    }

    public void setLastValue(double lastValue) {
        this.lastValue = lastValue;
    }
}
