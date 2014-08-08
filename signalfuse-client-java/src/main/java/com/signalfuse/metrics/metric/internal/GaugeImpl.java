package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.datumhandler.DatumHandler;
import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * @author jack
 */
public final class GaugeImpl extends DatumHandlerMetric implements Gauge {
    private final DoubleOrLong currentValue = new DoubleOrLong();

    public GaugeImpl(String source, String metric, DatumHandler datumHandler) {
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
    public static final class Factory extends BaseFactory<GaugeImpl> {
        public Factory(DatumHandler datumHandler) {
            super(datumHandler);
        }

        @Override
        protected GaugeImpl createItem(String sourceName, String metricName) {
            getDatumHandler().registerMetric(metricName,
                                             SignalFuseProtocolBuffers.MetricType.GAUGE);
            return new GaugeImpl(sourceName, metricName, getDatumHandler());
        }
    }
}
