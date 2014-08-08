package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.datumhandler.DatumHandler;
import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * @author jack
 */
public final class CounterImpl extends DatumHandlerMetric implements Counter, TrackedMetric {
    private final DoubleOrLong currentValue = new DoubleOrLong();

    private CounterImpl(String sourceName, String metricName, DatumHandler datumHandler) {
        super(sourceName, metricName, datumHandler);
    }

    @Override
    public void incr() {
        incr(1);
    }

    @Override
    public void incr(double amount) {
        currentValue.add(amount);
        getDatumHandler().addMetricValue(this, amount);
    }

    @Override
    public void incr(long amount) {
        currentValue.add(amount);
        getDatumHandler().addMetricValue(this, amount);
    }

    @Override
    public void decr() {
        incr(-1);
    }

    @Override
    public void decr(double amount) {
        incr(-amount);
    }

    @Override
    public void decr(long amount) {
        incr(-amount);
    }

    @Override
    public Number clearAndGetCurrentNumber() {
        return currentValue.clearAndGetNumber();
    }

    @Override
    public Number getValue() {
        return currentValue.getNumber();
    }

    public static final class Factory extends BaseFactory<CounterImpl> {
        public Factory(DatumHandler datumHandler) {
            super(datumHandler);
        }

        @Override
        protected CounterImpl createItem(String sourceName, String metricName) {
            getDatumHandler().registerMetric(metricName,
                    SignalFuseProtocolBuffers.MetricType.COUNTER);
            return new CounterImpl(sourceName, metricName, getDatumHandler());
        }
    }
}
