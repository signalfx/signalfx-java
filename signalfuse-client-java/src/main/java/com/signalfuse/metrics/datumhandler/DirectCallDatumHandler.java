package com.signalfuse.metrics.datumhandler;

import java.util.Collections;
import java.util.Set;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.auth.NoAuthTokenException;
import com.signalfuse.metrics.connection.DataPointReceiver;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.metric.Metric;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricError;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricErrorImpl;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricErrorType;
import com.signalfuse.metrics.metricbuilder.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;
import com.google.common.collect.ImmutableMap;

/**
 * Handler that doesn't wait and directly sends values when they are set
 *
 * @author jack
 */
public class DirectCallDatumHandler implements DatumHandler {
    private final DataPointReceiverFactory dataPointReceiverFactory;
    private final DataPointReceiverEndpoint endpoint;
    private final AuthToken authToken;
    private final Set<OnSendErrorHandler> onSendErrorHandler;
    private DataPointReceiver currentDataPointReceiver;

    public DirectCallDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                  DataPointEndpoint endpoint, AuthToken authToken,
                                  Set<OnSendErrorHandler> onSendErrorHandler) {
        this.dataPointReceiverFactory = dataPointReceiverFactory;
        this.endpoint = endpoint;
        this.authToken = authToken;
        this.onSendErrorHandler = onSendErrorHandler;
    }

    private void propogateError(MetricError error) {
        for (OnSendErrorHandler handler : onSendErrorHandler) {
            handler.handleError(error);
        }
    }

    private synchronized DataPointReceiver getCurrentDataPointReceiver()
            throws SignalfuseMetricsException {
        if (currentDataPointReceiver == null) {
            currentDataPointReceiver = dataPointReceiverFactory.createDataPointReceiver(endpoint);
        }
        return currentDataPointReceiver;
    }

    private synchronized void sendSingleDataPoint(SignalFuseProtocolBuffers.DataPoint toSend) {
        try {
            getCurrentDataPointReceiver().addDataPoints(authToken.getAuthToken(),
                    Collections.singletonList(toSend));
        } catch (NoAuthTokenException e) {
            propogateError(new MetricErrorImpl("Unable to find an auth token",
                    MetricErrorType.AUTH_TOKEN_ERROR, e));
        } catch (SignalfuseMetricsException e) {
            propogateError(new MetricErrorImpl("Unable to fully send datapoints",
                    MetricErrorType.DATAPOINT_SEND_ERROR, e));
        }
    }

    @Override public void addMetricValue(Metric metric, double value) {
        sendSingleDataPoint(SignalFuseProtocolBuffers.DataPoint.newBuilder()
                .setSource(metric.getSource()).setMetric(metric.getMetric())
                .setValue(SignalFuseProtocolBuffers.Datum.newBuilder().setDoubleValue(value))
                .build());
    }

    @Override public void addMetricValue(Metric metric, long value) {
        sendSingleDataPoint(SignalFuseProtocolBuffers.DataPoint.newBuilder()
                .setSource(metric.getSource()).setMetric(metric.getMetric())
                .setValue(SignalFuseProtocolBuffers.Datum.newBuilder().setIntValue(value)).build());
    }

    @Override public void registerMetric(String metricName, SignalFuseProtocolBuffers.MetricType metricType) {
        try {
            getCurrentDataPointReceiver().registerMetrics(authToken.getAuthToken(),
                    ImmutableMap.of(metricName, metricType));
        } catch (NoAuthTokenException e) {
            propogateError(new MetricErrorImpl("Unable to find an auth token",
                    MetricErrorType.AUTH_TOKEN_ERROR, e));
        } catch (SignalfuseMetricsException e) {
            propogateError(new MetricErrorImpl("Unable to fully send datapoints",
                    MetricErrorType.REGISTRATION_ERROR, e));
        }
    }

    public static class Factory implements DatumHandlerFactory {
        @Override public DatumHandler createDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                               DataPointEndpoint endpoint,
                                               AuthToken authToken,
                                               Set<OnSendErrorHandler> onSendErrorHandler) {
            return new DirectCallDatumHandler(dataPointReceiverFactory, endpoint, authToken,
                    onSendErrorHandler);
        }
    }
}
