package com.signalfx.codahale.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ResettingExponentiallyDecayingReservoir;
import com.codahale.metrics.Timer;

/**
 * An extension of {@link Timer} that resets its reservoir every time is snapshotted. This is
 * useful when you want to ignore the effects of very old data points on the current reported value.
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * MetricRegistry metrics = new MetricRegistry();
 * Timer timer = metrics.register("my.timer", new ResettingTimer());
 * try (Timer.Context ignored = timer.time()) {
 *     // do something.
 * }
 * </pre>
 *
 * @author uday
 */
public class ResettingTimer extends Timer {

    public ResettingTimer() {
        super(new ResettingExponentiallyDecayingReservoir());
    }

    public ResettingTimer(Clock clock) {
        super(new ResettingExponentiallyDecayingReservoir(), clock);
    }
}
