/**
 * Copyright (C) 2017 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.codahale.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.ResettingExponentiallyDecayingReservoir;

/**
 * A histogram that resets its reservoir every time it is snapshotted.
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * MetricRegistry metrics = new MetricRegistry();
 * Histogram histo = metrics.registry("my.histogram", new ResettingHistogram());
 * histo.update(42);
 * </pre>
 *
 * @author max
 * @see com.codahale.metrics.Histogram
 */
public class ResettingHistogram extends Histogram {

    public ResettingHistogram() {
        super(new ResettingExponentiallyDecayingReservoir());
    }

    public ResettingHistogram(int size, double alpha) {
        super(new ResettingExponentiallyDecayingReservoir(size, alpha));
    }

    public ResettingHistogram(int size, double alpha, Clock clock) {
        super(new ResettingExponentiallyDecayingReservoir(size, alpha, clock));
    }

    public final static class Builder implements MetricBuilder<ResettingHistogram> {
        public static final Builder INSTANCE = new Builder();

        @Override
        public ResettingHistogram newMetric() {
            return new ResettingHistogram();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return ResettingHistogram.class.isInstance(metric);
        }
    }
}
