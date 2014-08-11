package com.signalfuse.metrics.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Factory that just stores results to later be tested.
 * 
 * @author jack
 */
public class StoredDataPointReceiver implements DataPointReceiver {
    public final List<SignalFuseProtocolBuffers.DataPointOrBuilder> addDataPoints;
    public final Map<String, SignalFuseProtocolBuffers.MetricType> registeredMetrics;
    public boolean throwOnAdd = false;

    public StoredDataPointReceiver() {
        addDataPoints = Collections
                .synchronizedList(new ArrayList<SignalFuseProtocolBuffers.DataPointOrBuilder>());
        registeredMetrics = Collections.synchronizedMap(new HashMap<String, SignalFuseProtocolBuffers.MetricType>());
    }

    @Override
    public void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException {
        if (throwOnAdd) {
            throw new SignalfuseMetricsException("Flag set to true");
        }
        addDataPoints.addAll(dataPoints);
    }

    @Override
    public void backfillDataPoints(String auth, String source, String metric,
                                   List<SignalFuseProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException {}

    @Override
    public void registerMetrics(String auth,
                                Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException {
        registeredMetrics.putAll(metricTypes);
    }

}
