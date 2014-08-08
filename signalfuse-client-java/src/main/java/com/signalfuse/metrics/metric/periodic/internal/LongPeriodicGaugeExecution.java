package com.signalfuse.metrics.metric.periodic.internal;

import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.periodic.LongCallback;

/**
 * Package private class that handles executing and passing in gauge values
 * 
 * @author jack
 */
class LongPeriodicGaugeExecution extends PeriodicGaugeExecution {
    private final LongCallback callback;
    private long lastValue;

    LongPeriodicGaugeExecution(Gauge gauge, LongCallback callback) {
        super(gauge);
        this.callback = callback;
    }

    @Override
    public void run() {
        setLastValue(callback.getValue());
        getGauge().value(getLastValue());
    }

    public long getLastValue() {
        return lastValue;
    }

    public void setLastValue(long lastValue) {
        this.lastValue = lastValue;
    }
}
