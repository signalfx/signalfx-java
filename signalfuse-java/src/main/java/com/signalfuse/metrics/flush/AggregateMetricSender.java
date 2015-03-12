package com.signalfx.metrics.flush;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.auth.AuthToken;
import com.signalfx.metrics.auth.NoAuthTokenException;
import com.signalfx.metrics.connection.DataPointReceiver;
import com.signalfx.metrics.connection.DataPointReceiverFactory;
import com.signalfx.metrics.errorhandler.MetricErrorImpl;
import com.signalfx.metrics.errorhandler.MetricErrorType;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * The primary java class to send metrics.  To use this class, create a session, add points to
 * the session, and when you are done, close the session.  For example:
 *
 * <pre>
 * {@code
 *  AggregateMetricSender sender;
 *     try (AggregateMetricSender.Session i = mf.createSession()) {
 *         i.incrementCounter("testcounter2", 1);
 *         i.setDatapoint(
 *            SignalFxProtocolBuffers.DataPoint.newBuilder()
 *              .setMetric("curtime")
 *              .setValue(
 *                SignalFxProtocolBuffers.Datum.newBuilder()
 *                .setIntValue(System.currentTimeMillis()))
 *              .addDimensions(
 *                SignalFxProtocolBuffers.Dimension.newBuilder()
 *                  .setKey("source")
 *                  .setValue("java"))
 *              .build());
 *     }
 * }
 * </pre>
 */
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
                                  SignalFxMetricsException signalfxMetricsException) {
        for (OnSendErrorHandler onSendErrorHandler : onSendErrorHandlerCollection) {
            onSendErrorHandler
                    .handleError(new MetricErrorImpl(message, code, signalfxMetricsException));
        }
    }

    public Session createSession() {
        return new SessionImpl();
    }

    private final class SessionImpl implements Session {
        private final Map<String, com.signalfx.metrics.protobuf.SignalFxProtocolBuffers
                .MetricType> toBeRegisteredMetricPairs;
        private final List<SignalFxProtocolBuffers.DataPoint> pointsToFlush;

        private SessionImpl() {
            toBeRegisteredMetricPairs = new HashMap<String, com.signalfx.metrics.protobuf
                    .SignalFxProtocolBuffers.MetricType>();

            pointsToFlush = new ArrayList<SignalFxProtocolBuffers.DataPoint>();
        }

        public Session setCumulativeCounter(String metric, long value) {
            return setCumulativeCounter(defaultSourceName, metric, value);
        }

        public Session setCumulativeCounter(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, value);
            return this;
        }

        public Session incrementCounter(String metric, long value) {
            return incrementCounter(defaultSourceName, metric, value);
        }

        public Session incrementCounter(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFxProtocolBuffers.MetricType.COUNTER, value);
            return this;
        }

        @Override
        public Session setDatapoint(String source, String metric,
                                              SignalFxProtocolBuffers.MetricType metricType,
                                              long value) {
            check(metric, metricType);
            pointsToFlush.add(SignalFxProtocolBuffers.DataPoint.newBuilder()
                                      .setSource(source)
                                      .setMetricType(metricType)
                                      .setMetric(metric).setValue(
                            SignalFxProtocolBuffers.Datum.newBuilder().setIntValue(value).build())
                                      .build());
            return this;
        }

        @Override
        public Session setDatapoint(String source, String metric,
                                              SignalFxProtocolBuffers.MetricType metricType,
                                              double value) {
            check(metric, metricType);
            pointsToFlush.add(SignalFxProtocolBuffers.DataPoint.newBuilder()
                                      .setSource(source)
                                      .setMetricType(metricType)
                                      .setMetric(metric).setValue(
                            SignalFxProtocolBuffers.Datum.newBuilder().setDoubleValue(value).build())
                                      .build());
            return this;
        }

        @Override
        public Session setDatapoint(SignalFxProtocolBuffers.DataPoint datapoint) {
            check(datapoint.getMetric(), datapoint.getMetricType());
            pointsToFlush.add(datapoint);
            return this;
        }

        public Session setGauge(String metric, long value) {
            return setGauge(defaultSourceName, metric, value);
        }

        public Session setGauge(String source, String metric, long value) {
            setDatapoint(source, metric, SignalFxProtocolBuffers.MetricType.GAUGE, value);
            return this;
        }

        public Session setGauge(String metric, double value) {
            return setGauge(defaultSourceName, metric, value);
        }

        public Session setGauge(String source, String metric, double value) {
            setDatapoint(source, metric, SignalFxProtocolBuffers.MetricType.GAUGE, value);
            return this;
        }

        private void check(String metricPair,
                           com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType
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
                } catch (SignalFxMetricsException e) {
                    communicateError("Unable to register metrics",
                            MetricErrorType.REGISTRATION_ERROR,
                            e);
                    return;
                }
            }

            Iterator<SignalFxProtocolBuffers.DataPoint> i = pointsToFlush.iterator();
            while (i.hasNext()) {
                SignalFxProtocolBuffers.DataPoint currentEntry = i.next();
                if (!registeredMetricPairs.contains(currentEntry.getMetric())) {
                    i.remove();
                }
            }

            if (!pointsToFlush.isEmpty()) {
                try {
                    dataPointReceiver.addDataPoints(authTokenStr, pointsToFlush);
                } catch (SignalFxMetricsException e) {
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

        Session setDatapoint(String source, String metric, SignalFxProtocolBuffers.MetricType metricType, long value);

        Session setDatapoint(String source, String metric, SignalFxProtocolBuffers.MetricType metricType, double value);

        Session setDatapoint(SignalFxProtocolBuffers.DataPoint datapoint);
    }
}
