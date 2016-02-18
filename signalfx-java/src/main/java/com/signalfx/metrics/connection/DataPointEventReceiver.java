package com.signalfx.metrics.connection;

import java.util.List;
import java.util.Map;

import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public interface DataPointEventReceiver {
    void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalFxMetricsException;

    void backfillDataPoints(String auth, String source, String metric,
                            List<SignalFxProtocolBuffers.Datum> datumPoints)
            throws SignalFxMetricsException;

    Map<String, Boolean> registerMetrics(String auth, Map<String, SignalFxProtocolBuffers.MetricType> metricTypes)
            throws SignalFxMetricsException;

    void addEvents(String auth, List<SignalFxProtocolBuffers.Event> events)
            throws SignalFxMetricsException;
}
