package com.signalfuse.metrics.metric.periodic.internal;

import java.util.concurrent.ScheduledFuture;

/**
 * @author jack
 */
public abstract class PeriodicGaugeImpl {
    private final ScheduledFuture<?> myFuture;

    protected PeriodicGaugeImpl(ScheduledFuture<?> myFuture) {
        this.myFuture = myFuture;
    }

    public void cancel() {
        myFuture.cancel(false);
    }
}
