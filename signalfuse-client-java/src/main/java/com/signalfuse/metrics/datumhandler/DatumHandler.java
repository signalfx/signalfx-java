package com.signalfuse.metrics.datumhandler;

import com.signalfuse.metrics.metric.Metric;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Handles requests to change metric values
 * 
 * @author jack
 */
public interface DatumHandler {
    void addMetricValue(Metric metric, long value);

    void addMetricValue(Metric metric, double value);

    void registerMetric(String metricName, SignalFuseProtocolBuffers.MetricType metricType);
}
