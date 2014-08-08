package com.signalfuse.metrics.metric.periodic.internal;

import java.util.concurrent.ScheduledFuture;

import com.signalfuse.metrics.metric.periodic.LongPeriodicGauge;

/**
 * A periodic gauge that stores long values
 * 
 * @author jack
 */
public class LongPeriodicGaugeImpl extends PeriodicGaugeImpl implements LongPeriodicGauge {
    private final LongPeriodicGaugeExecution periodicGauge;

    public LongPeriodicGaugeImpl(ScheduledFuture<?> myFuture,
                                 LongPeriodicGaugeExecution periodicGauge) {
        super(myFuture);
        this.periodicGauge = periodicGauge;
    }

    @Override public long getLastValue() {
        return periodicGauge.getLastValue();
    }
}
