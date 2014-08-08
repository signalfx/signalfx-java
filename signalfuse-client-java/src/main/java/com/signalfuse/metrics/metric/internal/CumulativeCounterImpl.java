package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.datumhandler.DatumHandler;
import com.signalfuse.metrics.metric.CumulativeCounter;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * @author jack
 */
public class CumulativeCounterImpl extends DatumHandlerMetric implements CumulativeCounter {
    private final DoubleOrLong currentValue = new DoubleOrLong();

    private CumulativeCounterImpl(String source, String metric, DatumHandler datumHandler) {
        super(source, metric, datumHandler);
    }

    @Override
    public void value(long value) {
        currentValue.setValue(value);
        getDatumHandler().addMetricValue(this, value);
    }

    @Override
    public void value(double value) {
        currentValue.setValue(value);
        getDatumHandler().addMetricValue(this, value);
    }

    @Override
    public Number getValue() {
        return currentValue.getNumber();
    }

    public static final class Factory extends BaseFactory<CumulativeCounterImpl> {
        public Factory(DatumHandler datumHandler) {
            super(datumHandler);
        }

        @Override
        protected CumulativeCounterImpl createItem(String sourceName, String metricName) {
            getDatumHandler().registerMetric(metricName,
                    SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER);
            return new CumulativeCounterImpl(sourceName, metricName, getDatumHandler());
        }
    }
}
