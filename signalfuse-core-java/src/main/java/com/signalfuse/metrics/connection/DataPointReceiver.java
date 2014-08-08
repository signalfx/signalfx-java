package com.signalfuse.metrics.connection;

import java.util.List;
import java.util.Map;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public interface DataPointReceiver {
    void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException;

    void backfillDataPoints(String auth, String source, String metric,
                            List<SignalFuseProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException;

    void registerMetrics(String auth, Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException;
}
