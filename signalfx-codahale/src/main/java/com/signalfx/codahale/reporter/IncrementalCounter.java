package com.signalfx.codahale.reporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.signalfx.codahale.metrics.MetricBuilder;

/**
 * <p>
 * An {@link com.signalfx.codahale.reporter.IncrementalCounter} is a counter that reports
 * incremental values to SignalFx rather than absolute counts.  For example,
 * a regular {@link com.codahale.metrics.Counter} reports a monotonically increasing series of
 * values (1, 2, 3, 4, ...) while this class reports a series of increments (+1, +1, +1, +1), but
 * both represent the same rate of 1 unit per reporting interval. A
 * {@link com.codahale.metrics.Counter} created the regular Codahale way is the preferred way
 * to report incremental values to SignalFx when possible.
 * </p>
 * <p>
 * An example use case of this class would be if you wanted to count the number of requests to a webpage,
 * but didn't care about that as a dimension of the code serving the request.  So instead
 * of reporting source=hostname metric=webpage.user_login.hits", which multiplies by the
 * number of different source=hostname that are reporting, you can report the metric as
 * source=webpage metric=user_login.hits and all servers will increment the same metric, even though
 * they have different rolling counts.
 * </p>
 * <p>
 * A {@link com.codahale.metrics.Counter} assumes metric type
 * {@link com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType#CUMULATIVE_COUNTER},
 * while this class assumes metric type
 * {@link com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType#COUNTER}
 */
public class IncrementalCounter extends Counter {
    /**
     * The last value when {@link #getCountChange()} was called.
     */
    private long lastValue;

    /**
     * Returns the difference between the current value of the counter and the value when this
     * function was last called.
     *
     * @return Counter difference
     */
    public synchronized long getCountChange() {
        final long currentCount = getCount();
        final long countChange = currentCount - lastValue;
        lastValue = currentCount;
        return countChange;
    }

    public final static class Builder implements MetricBuilder<IncrementalCounter> {
        public static final Builder INSTANCE = new Builder();

        private Builder() {
        }

        @Override
        public IncrementalCounter newMetric() {
            return new IncrementalCounter();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return metric instanceof IncrementalCounter;
        }
    }
}
