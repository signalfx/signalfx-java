package com.signalfx.codahale.reporter;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

import java.util.concurrent.atomic.AtomicLong;

public abstract class SettableGauge<T> implements Metric, Gauge<T> {

    private final AtomicLong lastSet = new AtomicLong();
    private volatile long lastReported = 0;

    /**
     * Mark the gauge has having been set.
     */
    protected void markSet() {
        lastSet.incrementAndGet();
    }

    /**
     * Mark the gauge has having been reported.
     */
    void markReported() {
        lastReported = lastSet.get();
    }

    /**
     * @return Returns <em>true</em> iff the gauge's value has been set since the last time it was
     *      reported.
     */
    boolean hasChanged() {
        return lastSet.get() > lastReported;
    }
}
