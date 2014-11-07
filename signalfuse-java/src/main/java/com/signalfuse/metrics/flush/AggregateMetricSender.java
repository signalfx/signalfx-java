package com.signalfuse.metrics.flush;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.auth.NoAuthTokenException;
import com.signalfuse.metrics.connection.DataPointReceiver;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.errorhandler.MetricErrorImpl;
import com.signalfuse.metrics.errorhandler.MetricErrorType;
import com.signalfuse.metrics.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public class AggregateMetricSender {
    private final String defaultSourceName;
    private final Set<String> registeredMetricPairs;
    private final DataPointReceiverFactory dataPointReceiverFactory;
    private final AuthToken authToken;
    private final Collection<OnSendErrorHandler> onSendErrorHandlerCollection;

    public AggregateMetricSender(String defaultSourceName, DataPointReceiverFactory dataPointReceiverFactory,
                                 AuthToken authToken,
                                 Collection<OnSendErrorHandler> onSendErrorHandlerCollection) {
        this.defaultSourceName = defaultSourceName;
        registeredMetricPairs = new HashSet<String>();
        this.dataPointReceiverFactory = dataPointReceiverFactory;
        this.authToken = authToken;
        this.onSendErrorHandlerCollection = onSendErrorHandlerCollection;
    }

    public String getDefaultSourceName() {
        return defaultSourceName;
    }

    private void communicateError(String message, MetricErrorType code,
                                  SignalfuseMetricsException signalfuseMetricsException) {
        for (OnSendErrorHandler onSendErrorHandler : onSendErrorHandlerCollection) {
            onSendErrorHandler
                    .handleError(new MetricErrorImpl(message, code, signalfuseMetricsException));
        }
    }

    public Session createSession() {
        return new SessionImpl();
    }

    private final class SessionImpl implements Session {
        private final Map<String, com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers
                .MetricType> toBeRegisteredMetricPairs;
        private final List<SignalFuseProtocolBuffers.DataPoint> pointsToFlush;

        private SessionImpl() {
            toBeRegisteredMetricPairs = new HashMap<String, com.signalfuse.metrics.protobuf
                    .SignalFuseProtocolBuffers.MetricType>();

            pointsToFlush = new ArrayList<SignalFuseProtocolBuffers.DataPoint>();
        }

        public Session setCumulativeCounter(String metric, long value) {
            return setCumulativeCounter(defaultSourceName, metric, value);
        }

        public Session setCumulativeCounter(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER, value);
            return this;
        }

        public Session incrementCounter(String metric, long value) {
            return incrementCounter(defaultSourceName, metric, value);
        }

        public Session incrementCounter(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFuseProtocolBuffers.MetricType.COUNTER, value);
            return this;
        }

        @Override
        public Session setDatapoint(String source, String metric,
                                              SignalFuseProtocolBuffers.MetricType metricType,
                                              long value) {
            check(metric, metricType);
            addDatapoint(source, metric, value);
            return this;
        }

        @Override
        public Session setDatapoint(String source, String metric,
                                              SignalFuseProtocolBuffers.MetricType metricType,
                                              double value) {
            check(metric, metricType);
            addDatapoint(source, metric, value);
            return this;
        }

        @Override
        public Session setDatapoint(SignalFuseProtocolBuffers.DataPoint datapoint) {
            check(datapoint.getMetric(), datapoint.getMetricType());
            pointsToFlush.add(datapoint);
            return this;
        }

        public Session setGauge(String metric, long value) {
            return setGauge(defaultSourceName, metric, value);
        }

        public Session setGauge(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFuseProtocolBuffers.MetricType.GAUGE, value);
            return this;
        }

        public Session setGauge(String metric, double value) {
            return setGauge(defaultSourceName, metric, value);
        }

        public Session setGauge(String source, String metric, double value) {
            setDatapoint(source, metric, SignalFuseProtocolBuffers.MetricType.GAUGE, value);
            return this;
        }

        private void addDatapoint(String source, String metric, double value) {
            pointsToFlush.add(SignalFuseProtocolBuffers.DataPoint.newBuilder().setSource(source)
                    .setMetric(metric).setValue(
                            SignalFuseProtocolBuffers.Datum.newBuilder().setDoubleValue(value)
                                    .build())
                    .build());
        }

        private void addDatapoint(String source, String metric, long value) {
            pointsToFlush.add(SignalFuseProtocolBuffers.DataPoint.newBuilder().setSource(source)
                    .setMetric(metric).setValue(
                            SignalFuseProtocolBuffers.Datum.newBuilder().setIntValue(value).build())
                    .build());
        }

        private void check(String metricPair,
                           com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers.MetricType
                                   metricType) {
            if (!registeredMetricPairs.contains(metricPair)) {
                toBeRegisteredMetricPairs.put(metricPair, metricType);
            }
        }

        public void close() {
            final String authTokenStr;
            try {
                authTokenStr = authToken.getAuthToken();
            } catch (NoAuthTokenException e) {
                communicateError("Unable to get auth token", MetricErrorType.AUTH_TOKEN_ERROR,
                        e);
                return;
            }

            DataPointReceiver dataPointReceiver = dataPointReceiverFactory.createDataPointReceiver();

            if (!toBeRegisteredMetricPairs.isEmpty()) {
                try {
                    Map<String, Boolean> registeredPairs = dataPointReceiver.registerMetrics(authTokenStr,
                            toBeRegisteredMetricPairs);
                    for (Map.Entry<String, Boolean> i: registeredPairs.entrySet()) {
                        if (i.getValue()) {
                            registeredMetricPairs.add(i.getKey());
                        }
                    }
                } catch (SignalfuseMetricsException e) {
                    communicateError("Unable to register metrics",
                            MetricErrorType.REGISTRATION_ERROR,
                            e);
                    return;
                }
            }

            Iterator<SignalFuseProtocolBuffers.DataPoint> i = pointsToFlush.iterator();
            while (i.hasNext()) {
                SignalFuseProtocolBuffers.DataPoint currentEntry = i.next();
                if (!registeredMetricPairs.contains(currentEntry.getMetric())) {
                    i.remove();
                }
            }

            if (!pointsToFlush.isEmpty()) {
                try {
                    dataPointReceiver.addDataPoints(authTokenStr, pointsToFlush);
                } catch (SignalfuseMetricsException e) {
                    communicateError("Unable to send datapoints",
                            MetricErrorType.DATAPOINT_SEND_ERROR,
                            e);
                    return;
                }
            }
        }
    }

    public interface Session extends Closeable {
        Session setCumulativeCounter(String metric, long value);

        Session setCumulativeCounter(String source, String metric, long value);

        Session setGauge(String metric, long value);

        Session setGauge(String source, String metric, long value);

        Session setGauge(String metric, double value);

        Session setGauge(String source, String metric, double value);

        Session incrementCounter(String metric, long value);

        Session incrementCounter(String source, String metric, long value);

        Session setDatapoint(String source, String metric, SignalFuseProtocolBuffers.MetricType metricType, long value);

        Session setDatapoint(String source, String metric, SignalFuseProtocolBuffers.MetricType metricType, double value);

        Session setDatapoint(SignalFuseProtocolBuffers.DataPoint datapoint);
    }
}
