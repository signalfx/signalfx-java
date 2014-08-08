package com.signalfuse.metrics.metric.periodic.internal;

import java.util.concurrent.ScheduledFuture;

import com.signalfuse.metrics.metric.periodic.DoublePeriodicGauge;

/**
 * A periodic gauge that stores double values
 * 
 * @author jack
 */
public class DoublePeriodicGaugeImpl extends PeriodicGaugeImpl implements DoublePeriodicGauge {
    private final DoublePeriodicGaugeExecution periodicGauge;

    public DoublePeriodicGaugeImpl(ScheduledFuture<?> myFuture,
                                   DoublePeriodicGaugeExecution periodicGauge) {
        super(myFuture);
        this.periodicGauge = periodicGauge;
    }

    @Override public double getLastValue() {
        return periodicGauge.getLastValue();
    }
}
