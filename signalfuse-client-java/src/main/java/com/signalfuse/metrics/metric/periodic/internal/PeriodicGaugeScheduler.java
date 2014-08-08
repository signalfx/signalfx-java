package com.signalfuse.metrics.metric.periodic.internal;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.periodic.DoubleCallback;
import com.signalfuse.metrics.metric.periodic.DoublePeriodicGauge;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.metric.periodic.LongPeriodicGauge;

/**
 * Handles execution of periodic gauges
 * 
 * @author jack
 */
public class PeriodicGaugeScheduler {
    public static final int CORE_POOL_SIZE = 1;
    private final ScheduledThreadPoolExecutor timer;

    public PeriodicGaugeScheduler() {
        ThreadFactory tf = new BasicThreadFactory.Builder().daemon(true)
            .namingPattern("PeriodicGaugeWorker-%d").build();
        this.timer = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, tf);
    }

    public LongPeriodicGauge addPeriodicGauge(final Gauge gauge, TimeUnit timeUnit,
                                              long unitPeriod, final LongCallback callback) {
        LongPeriodicGaugeExecution periodicGauge = new LongPeriodicGaugeExecution(gauge, callback);
        ScheduledFuture<?> myFuture = timer.scheduleWithFixedDelay(periodicGauge, 0, unitPeriod,
            timeUnit);
        return new LongPeriodicGaugeImpl(myFuture, periodicGauge);
    }

    public DoublePeriodicGauge addPeriodicGauge(final Gauge gauge, TimeUnit timeUnit,
                                                long unitPeriod, final DoubleCallback callback) {
        DoublePeriodicGaugeExecution periodicGauge = new DoublePeriodicGaugeExecution(gauge,
            callback);
        ScheduledFuture<?> myFuture = timer.scheduleWithFixedDelay(periodicGauge, 0, unitPeriod,
            timeUnit);
        return new DoublePeriodicGaugeImpl(myFuture, periodicGauge);
    }
}
