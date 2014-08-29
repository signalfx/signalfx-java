package com.signalfuse.codahale.metrics;

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
 *       SettableLongGauge settable = metricRegistry.register("metric.name", new SettableLongGauge());
 *       // ...
 *       settable.setValue(100);
 *       // ...
 *       settable.setValue(200);
 *     }
 *
 *     </pre>
 */
public class SettableLongGauge implements Metric, Gauge<Long> {
    /**
     * Current value.  Assignment will be atomic.  <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.7">See 17.7</a>
     */
    private volatile long value = 0;

    /**
     * Set the current value the {@link Gauge} will return to something else.
     * @param value    last set value
     * @return itself
     */
    public SettableLongGauge setValue(long value) {
        this.value = value;
        return this;
    }

    /**
     * The last value set by {@link #setValue(long)}}
     * @return Last set value, or zero.
     */
    public Long getValue() {
        return value;
    }
}
