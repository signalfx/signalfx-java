package com.signalfuse.metrics.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Factory that just stores results to later be tested.
 * 
 * @author jack
 */
public class StoredDataPointReceiver implements DataPointReceiver {
    public final List<SignalFuseProtocolBuffers.DataPointOrBuilder> addDataPoints;
    private final Map<Pair<String, String>, List<SignalFuseProtocolBuffers.Datum>> pointsFor;
    public final Map<String, SignalFuseProtocolBuffers.MetricType> registeredMetrics;
    public boolean throwOnAdd = false;

    public StoredDataPointReceiver() {
        addDataPoints = Collections
                .synchronizedList(new ArrayList<SignalFuseProtocolBuffers.DataPointOrBuilder>());
        registeredMetrics = Collections.synchronizedMap(new HashMap<String, SignalFuseProtocolBuffers.MetricType>());

        pointsFor = Maps.newHashMap();
    }

    @Override
    public void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException {
        if (throwOnAdd) {
            throw new SignalfuseMetricsException("Flag set to true");
        }
        addDataPoints.addAll(dataPoints);
        for (SignalFuseProtocolBuffers.DataPoint dp: dataPoints) {
            Pair<String, String> key = Pair.of(dp.getSource(), dp.getMetric());
            if (pointsFor.containsKey(key)) {
                pointsFor.get(key).add(dp.getValue());
            } else {
                pointsFor.put(key, Lists.newArrayList(dp.getValue()));
            }
        }
    }

    @Override
    public void backfillDataPoints(String auth, String source, String metric,
                                   List<SignalFuseProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException {}

    @Override
    public Map<String, Boolean> registerMetrics(String auth,
                                Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException {
        registeredMetrics.putAll(metricTypes);
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> i: metricTypes.entrySet()) {
            ret.put(i.getKey(), true);
        }
        return ret;
    }

    public List<SignalFuseProtocolBuffers.Datum> valuesFor(String source, String metric) {
        Pair<String, String> key = Pair.of(source, metric);
        List<SignalFuseProtocolBuffers.Datum> ret = pointsFor.get(key);
        if (ret == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(ret);
        }
    }

    public SignalFuseProtocolBuffers.Datum lastValueFor(String source, String metric) {
        List<SignalFuseProtocolBuffers.Datum> vals = valuesFor(source, metric);
        if (vals.isEmpty()) {
            throw new RuntimeException("No value for source/metric");
        } else {
            return vals.get(vals.size() - 1);
        }
    }
}
