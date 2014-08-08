package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.datumhandler.DatumHandler;
import com.signalfuse.metrics.metric.Sample;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public final class SampleImpl extends DatumHandlerMetric implements Sample {
    private final DoubleOrLong currentValue = new DoubleOrLong();

    public SampleImpl(String source, String metric, DatumHandler datumHandler) {
        super(source, metric, datumHandler);
    }

    @Override
    public Timer time() {
        return new TimerImpl();
    }

    public final class TimerImpl implements Timer {
        private final long startTimeNano;
        private boolean hasBeenClosed;

        private TimerImpl() {
            startTimeNano = System.nanoTime();
            hasBeenClosed = false;
        }

        @Override
        public void close() {
            if (hasBeenClosed) {
                throw new RuntimeException("Create a new latencywatcher for each block");
            }
            hasBeenClosed = true;
            final long difference = System.nanoTime() - startTimeNano;
            addSample(difference);
        }
    }

    @Override
    public void addSample(double value) {
        currentValue.setValue(value);
        getDatumHandler().addMetricValue(this, value);
    }

    @Override
    public void addSample(long value) {
        currentValue.setValue(value);
        getDatumHandler().addMetricValue(this, value);
    }

    @Override
    public Number getValue() {
        return currentValue.getNumber();
    }

    public static final class Factory extends BaseFactory<SampleImpl> {
        public Factory(DatumHandler datumHandler) {
            super(datumHandler);
        }

        @Override
        protected SampleImpl createItem(String sourceName, String metricName) {
            getDatumHandler().registerMetric(metricName,
                                             SignalFuseProtocolBuffers.MetricType.GAUGE);
            return new SampleImpl(sourceName, metricName, getDatumHandler());
        }
    }
}
