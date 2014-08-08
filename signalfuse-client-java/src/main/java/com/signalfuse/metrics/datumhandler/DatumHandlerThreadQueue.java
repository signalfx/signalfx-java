package com.signalfuse.metrics.datumhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.auth.NoAuthTokenException;
import com.signalfuse.metrics.connection.DataPointReceiver;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.metric.Metric;
import com.signalfuse.metrics.metric.internal.TrackedMetric;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricError;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricErrorImpl;
import com.signalfuse.metrics.metricbuilder.errorhandler.MetricErrorType;
import com.signalfuse.metrics.metricbuilder.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * A DatumHandler that will queue up results and send them in batches as fast as possible, with
 * exponential backoff.
 * 
 * @author jack
 */
public class DatumHandlerThreadQueue implements DatumHandler, Runnable {

    private static final Logger log = LoggerFactory.getLogger(DatumHandlerThreadQueue.class);
    private static final int BLOCKING_QUEUE_INIT_SIZE = 10000;
    private final DataPointReceiverFactory dataPointReceiverFactory;
    private final DataPointEndpoint endpoint;
    private final AuthToken authToken;
    private final Set<OnSendErrorHandler> onSendErrorHandler;
    private final BlockingQueue<SignalFuseProtocolBuffers.DataPoint> toSendQueue;
    private final BlockingQueue<Pair<String, SignalFuseProtocolBuffers.MetricType>> toRegisterTimeSeries;
    private final Set<TrackedMetric> trackedAbsoluteCounters;
    private final Set<TrackedMetric> trackedRelativeCounters;
    private final long datapointHeartbeatMs;

    public DatumHandlerThreadQueue(DataPointReceiverFactory dataPointReceiverFactory,
                                   DataPointEndpoint endpoint, AuthToken authToken,
                                   Set<OnSendErrorHandler> onSendErrorHandler,
                                   long millisecondDelayBetweenPoints) {
        this.dataPointReceiverFactory = dataPointReceiverFactory;
        this.endpoint = endpoint;
        this.authToken = authToken;
        this.onSendErrorHandler = onSendErrorHandler;
        this.toSendQueue = new ArrayBlockingQueue<SignalFuseProtocolBuffers.DataPoint>(BLOCKING_QUEUE_INIT_SIZE);
        trackedAbsoluteCounters = Collections
                .newSetFromMap(new ConcurrentHashMap<TrackedMetric, Boolean>());
        trackedRelativeCounters = Collections
                .newSetFromMap(new ConcurrentHashMap<TrackedMetric, Boolean>());
        this.datapointHeartbeatMs = millisecondDelayBetweenPoints;
        this.toRegisterTimeSeries = new ArrayBlockingQueue<Pair<String, SignalFuseProtocolBuffers.MetricType>>(BLOCKING_QUEUE_INIT_SIZE);
    }

    private static void drainCounters(List<SignalFuseProtocolBuffers.DataPoint> ddpList,
                                      Set<TrackedMetric> metrics) {
        for (TrackedMetric metric : metrics) {
            final Number currentVal = metric.clearAndGetCurrentNumber();
            if (currentVal == null) {
                continue;
            }
            SignalFuseProtocolBuffers.Datum.Builder datumBuilder = SignalFuseProtocolBuffers.Datum
                    .newBuilder();
            if (currentVal instanceof Long || currentVal instanceof Integer
                    || currentVal instanceof Short) {
                datumBuilder.setIntValue(currentVal.longValue());
            } else {
                datumBuilder.setDoubleValue(currentVal.doubleValue());
            }
            SignalFuseProtocolBuffers.Datum value = datumBuilder.build();
            ddpList.add(SignalFuseProtocolBuffers.DataPoint.newBuilder()
                                                 .setMetric(metric.getMetric())
                                                 .setSource(metric.getSource()).setValue(value)
                                                 .build());
        }
    }

    private void propogateError(MetricError error) {
        log.trace("propogating error: {}", error);
        for (OnSendErrorHandler handler: onSendErrorHandler) {
            handler.handleError(error);
        }
    }

    private void drainOutstandingMetrics() throws InterruptedException, NoAuthTokenException {
        while (!toRegisterTimeSeries.isEmpty()) {
            final Map<String, SignalFuseProtocolBuffers.MetricType> metricTypesToRegister = new HashMap<String, SignalFuseProtocolBuffers.MetricType>();
            final List<Pair<String, SignalFuseProtocolBuffers.MetricType>> L = new ArrayList<Pair<String, SignalFuseProtocolBuffers.MetricType>>();
            L.add(toRegisterTimeSeries.take());
            // Once we can multi register, we can add this back.  Until then, only register
            // one at a time so we can more easily remove things from our back queue
            //toRegisterTimeSeries.drainTo(L);

            for (Pair<String, SignalFuseProtocolBuffers.MetricType> i : L) {
                metricTypesToRegister.put(i.getKey(), i.getValue());
            }
            if (log.isTraceEnabled()) {
                for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> ts : metricTypesToRegister
                        .entrySet()) {
                    log.trace("Registering ts {}", ts);
                }
            }
            try {
                final DataPointReceiver currentDataPointReceiver;
                try {
                    currentDataPointReceiver = getInterface();
                } catch (SignalfuseMetricsException e) {
                    propogateError(new MetricErrorImpl("Failed to connect",
                                                       MetricErrorType.CONNECTION_ERROR, e));
                    TimeUnit.MILLISECONDS.sleep(datapointHeartbeatMs);
                    continue;
                }
                currentDataPointReceiver.registerMetrics(authToken.getAuthToken(),
                        metricTypesToRegister);
                log.trace("Register finished!");
                break;
            } catch (SignalfuseMetricsException e) {
                propogateError(new MetricErrorImpl("Unable to register time series.",
                                                   MetricErrorType.REGISTRATION_ERROR, e));
                for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> ts : metricTypesToRegister
                        .entrySet()) {
                    loggedOffer(toRegisterTimeSeries, Pair.of(ts.getKey(), ts.getValue()));
                }
                TimeUnit.MILLISECONDS.sleep(datapointHeartbeatMs);
            }
        }
    }

    /**
     * A drain for the addDataPoints endpoint
     */
    @Override public void run() {
        final List<SignalFuseProtocolBuffers.DataPoint> ddpList = new ArrayList<SignalFuseProtocolBuffers.DataPoint>(
                BLOCKING_QUEUE_INIT_SIZE + 1);
        DataPointReceiver currentDataPointReceiver = null;

        while (true) {
            ddpList.clear();
            try {
                TimeUnit.MILLISECONDS.sleep(datapointHeartbeatMs);
            } catch (InterruptedException e) {
                propogateError(new MetricErrorImpl("Thread interrupted", MetricErrorType.INTERUPTED, new SignalfuseMetricsException(e)));
                log.info("Interrupted.  Shutting down", e);
                return;
            }
            if (currentDataPointReceiver == null) {
                try {
                    currentDataPointReceiver = getInterface();
                } catch (SignalfuseMetricsException e) {
                    propogateError(
                            new MetricErrorImpl("Failed to connect", MetricErrorType.CONNECTION_ERROR,
                                                e));
                    continue;
                }
            }
            toSendQueue.drainTo(ddpList);
            drainCounters(ddpList, trackedAbsoluteCounters);
            drainCounters(ddpList, trackedRelativeCounters);
            if (ddpList.isEmpty()) {
                continue;
            }
            // It is very important to drain outstanding metrics that need to be created before we
            // send any datapoints, otherwise our counters may be auto created as a gauge
            try {
                drainOutstandingMetrics();
            } catch (InterruptedException e) {
                propogateError(new MetricErrorImpl("Thread interrupted", MetricErrorType.INTERUPTED,
                                                   new SignalfuseMetricsException(e)));
                return;
            }
            try {
                try {
                    log.trace("Draining {} datapoints", ddpList.size());
                    currentDataPointReceiver.addDataPoints(authToken.getAuthToken(), ddpList);
                } catch (NoAuthTokenException e) {
                    propogateError(new MetricErrorImpl("Unable to find an auth token",
                                                       MetricErrorType.AUTH_TOKEN_ERROR, e));
                }
            } catch (SignalfuseMetricsException e) {
                // ambiguous if I should add the points. addDataPoints is not atomic. We could
                // double add points
                currentDataPointReceiver = null; // Null it out so I create a new one next time
                propogateError(new MetricErrorImpl("Unable to fully send datapoints", MetricErrorType.DATAPOINT_SEND_ERROR, e));
            }
        }
    }

    private DataPointReceiver getInterface() throws SignalfuseMetricsException {
        return dataPointReceiverFactory.createDataPointReceiver(endpoint);
    }

    @Override public void addMetricValue(Metric metric, long value) {
        if (!addTracked(metric)) {
            loggedOffer(toSendQueue,
                    SignalFuseProtocolBuffers.DataPoint.newBuilder().setMetric(metric.getMetric())
                            .setSource(metric.getSource()).setValue(createDatum(value)).build());
        }
    }

    @Override public void addMetricValue(Metric metric, double value) {
        if (!addTracked(metric)) {
            loggedOffer(toSendQueue,
                    SignalFuseProtocolBuffers.DataPoint.newBuilder().setMetric(metric.getMetric())
                            .setSource(metric.getSource()).setValue(createDatum(value)).build());
        }
    }

    public boolean addTracked(Metric metric) {
        if (metric instanceof TrackedMetric && metric instanceof Counter) {
            trackedRelativeCounters.add((TrackedMetric) metric);
            return true;
        } else {
            return false;
        }
    }

    @Override public void registerMetric(String metricName, SignalFuseProtocolBuffers.MetricType metricType) {
        Pair<String, SignalFuseProtocolBuffers.MetricType> item = Pair.of(metricName, metricType);
        loggedOffer(toRegisterTimeSeries, item);
    }

    private static SignalFuseProtocolBuffers.Datum createDatum(long n) {
        return SignalFuseProtocolBuffers.Datum.newBuilder().setIntValue(n).build();
    }

    private static SignalFuseProtocolBuffers.Datum createDatum(double n) {
        return SignalFuseProtocolBuffers.Datum.newBuilder().setDoubleValue(n).build();
    }

    private <T> void loggedOffer(BlockingQueue<T> items, T item) {
        if (!items.offer(item)) {
            propogateError(new MetricErrorImpl("Local queue too full to add another item",
                                               MetricErrorType.QUEUE_FULL, null));
        }
    }

    public static final class Factory implements DatumHandlerFactory {
        public static final long DEFAULT_DELAY_MS = 1000;
        private long millisecondDelayBetweenPoints = DEFAULT_DELAY_MS;

        public Factory setDatumHandlerBufferDelay(long millisecondDelayBetweenPoints) {
            this.millisecondDelayBetweenPoints = millisecondDelayBetweenPoints;
            return this;
        }

        @Override public DatumHandler createDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                               DataPointEndpoint endpoint,
                                               AuthToken authToken,
                                               Set<OnSendErrorHandler> onSendErrorHandler) {
            DatumHandlerThreadQueue dh = new DatumHandlerThreadQueue(dataPointReceiverFactory,
                    endpoint, authToken, onSendErrorHandler, millisecondDelayBetweenPoints);
            Thread t = new Thread(dh, "com.signalfuse.signalfuse-datum-handler-thread");
            t.setDaemon(true);
            t.start();
            return dh;
        }
    }
}
