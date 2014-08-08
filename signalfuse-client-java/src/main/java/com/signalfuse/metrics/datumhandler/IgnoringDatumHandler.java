package com.signalfuse.metrics.datumhandler;

import java.util.Set;

import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.metric.Metric;
import com.signalfuse.metrics.metricbuilder.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Ignores all datum sent to it
 * 
 * @author jack
 */
public class IgnoringDatumHandler implements DatumHandler {

    @Override public void addMetricValue(Metric metric, long value) {
        // Ignore
    }

    @Override public void addMetricValue(Metric metric, double value) {
        // Ignore
    }

    @Override public void registerMetric(String metricName, SignalFuseProtocolBuffers.MetricType metricType) {
        // Ignore
    }

    public static class Factory implements DatumHandlerFactory {

        @Override public DatumHandler createDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                               DataPointEndpoint endpoint,
                                               AuthToken authToken,
                                               Set<OnSendErrorHandler> onSendErrorHandler) {
            return new IgnoringDatumHandler();
        }
    }
}
