package com.signalfuse.codahale.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;

/**
 * <p>
 * Sometimes you want the rate of something like you would with a {@link Counter}, but you can't get
 * individual events and instead must set a total "count" of events at some periodic rate.  This
 * class abstracts that out into a {@link Counter} that codahale can understand.
 * </p>
 */
public class CallbackCumulativeCounter extends Counter {
    private final Callback callback;

    public CallbackCumulativeCounter(Callback callback) {
        this.callback = callback;
    }

    @Override public void inc() {
        throw new UnsupportedOperationException("inc() on CallbackCumulativeCounter");
    }

    @Override public void inc(long n) {
        throw new UnsupportedOperationException("inc() on CallbackCumulativeCounter");
    }

    @Override public void dec() {
        throw new UnsupportedOperationException("dec() on CallbackCumulativeCounter");
    }

    @Override public void dec(long n) {
        throw new UnsupportedOperationException("dec() on CallbackCumulativeCounter");
    }

    @Override public long getCount() {
        return callback.getValue();
    }

    public interface Callback extends Gauge<Long> {
    }
}
