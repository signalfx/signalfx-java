package com.signalfx.metrics.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.Dimension;

/**
 * Factory that just stores results to later be tested.
 *
 * @author jack
 */
public class StoredDataPointReceiver implements DataPointReceiver {
    public final List<SignalFxProtocolBuffers.DataPointOrBuilder> addDataPoints;
    private final Map<Pair<String, String>, List<SignalFxProtocolBuffers.Datum>> pointsFor;
    public final Map<String, SignalFxProtocolBuffers.MetricType> registeredMetrics;
    public boolean throwOnAdd = false;

    public StoredDataPointReceiver() {
        addDataPoints = Collections
                .synchronizedList(new ArrayList<SignalFxProtocolBuffers.DataPointOrBuilder>());
        registeredMetrics = Collections.synchronizedMap(new HashMap<String, SignalFxProtocolBuffers.MetricType>());

        pointsFor = Maps.newHashMap();
    }

    @Override
    public void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalFxMetricsException {
        if (throwOnAdd) {
            throw new SignalFxMetricsException("Flag set to true");
        }
        addDataPoints.addAll(dataPoints);
        for (SignalFxProtocolBuffers.DataPoint dp: dataPoints) {
            String source = dp.getSource();
            if ("".equals(source)) {
                source = findSfSourceDim(dp.getDimensionsList());
            }
            Pair<String, String> key = Pair.of(source, dp.getMetric());
            if (pointsFor.containsKey(key)) {
                pointsFor.get(key).add(dp.getValue());
            } else {
                pointsFor.put(key, Lists.newArrayList(dp.getValue()));
            }
        }
    }

    private String findSfSourceDim(List<Dimension> dimensionsList) {
        for (Dimension dim: dimensionsList) {
            if ("sf_source".equals(dim.getKey())) {
                return dim.getValue();
            }
        }
        return "";
    }

    @Override
    public void backfillDataPoints(String auth, String metric, String metricType, String orgId, Map<String,String> dimensions,
                                   List<SignalFxProtocolBuffers.PointValue> datumPoints)
            throws SignalFxMetricsException {}

    @Override
    public Map<String, Boolean> registerMetrics(String auth,
                                Map<String, SignalFxProtocolBuffers.MetricType> metricTypes)
            throws SignalFxMetricsException {
        registeredMetrics.putAll(metricTypes);
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFxProtocolBuffers.MetricType> i: metricTypes.entrySet()) {
            ret.put(i.getKey(), true);
        }
        return ret;
    }

    public List<SignalFxProtocolBuffers.Datum> valuesFor(String source, String metric) {
        Pair<String, String> key = Pair.of(source, metric);
        List<SignalFxProtocolBuffers.Datum> ret = pointsFor.get(key);
        if (ret == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(ret);
        }
    }

    public SignalFxProtocolBuffers.Datum lastValueFor(String source, String metric) {
        List<SignalFxProtocolBuffers.Datum> vals = valuesFor(source, metric);
        if (vals.isEmpty()) {
            throw new RuntimeException("No value for source/metric");
        } else {
            return vals.get(vals.size() - 1);
        }
    }

    public boolean clearValues(String source, String metric) {
        Pair<String, String> key = Pair.of(source, metric);
        return pointsFor.remove(key) != null;
    }
}
