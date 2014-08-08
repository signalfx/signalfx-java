package com.signalfuse.metrics.metric.periodic.internal;

import com.signalfuse.metrics.metric.Gauge;

/**
 * Package private base class for gauge values.
 * 
 * @author jack
 */
abstract class PeriodicGaugeExecution implements Runnable {
    private final Gauge gauge;

    protected PeriodicGaugeExecution(Gauge gauge) {
        this.gauge = gauge;
    }

    protected Gauge getGauge() {
        return gauge;
    }
}
