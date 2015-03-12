package com.signalfx.codahale.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

/**
 * <p>
 * Works like a Gauge, but rather than getting its value from a callback, the value
 * is set when needed.  This can be somewhat convienent, but direct use of a Gauge is likely better
 * </p>
 * <p>
 *     Usage example:
 *     <pre>{@code
 *       MetricRegister metricRegistry;
 *       SettableDoubleGauge settable = metricRegistry.register("metric.name", new SettableDoubleGauge());
 *       // ...
 *       settable.setValue(1.234);
 *       // ...
 *       settable.setValue(3.156);
 *     }
 *     </pre>
 */
public class SettableDoubleGauge implements Metric, Gauge<Double> {
    /**
     * Current value.  Assignment will be atomic.  <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.7">See 17.7</a>
     */
    private volatile double value;

    /**
     * Set the current value the {@link Gauge} will return to something else.
     * @param value    last set value
     * @return itself
     */
    public SettableDoubleGauge setValue(double value) {
        this.value = value;
        return this;
    }

    /**
     * The last value set by {@link #setValue(double)}}
     * @return Last set value, or zero.
     */
    public Double getValue() {
        return value;
    }


    public final static class Builder implements MetricBuilder<SettableDoubleGauge> {
        public static final Builder INSTANCE = new Builder();
        private Builder() {
        }

        @Override
        public SettableDoubleGauge newMetric() {
            return new SettableDoubleGauge();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return metric instanceof SettableDoubleGauge;
        }
    }
}
