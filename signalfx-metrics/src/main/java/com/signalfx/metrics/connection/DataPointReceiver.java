package com.signalfx.metrics.connection;

import java.util.List;
import java.util.Map;

import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public interface DataPointReceiver {
    void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalFxMetricsException;

    void backfillDataPoints(String auth, String metric, String metricType, String orgId, Map<String,String> dimensions,
                            List<SignalFxProtocolBuffers.PointValue> datumPoints)
            throws SignalFxMetricsException;

    Map<String, Boolean> registerMetrics(String auth, Map<String, SignalFxProtocolBuffers.MetricType> metricTypes)
            throws SignalFxMetricsException;
}
