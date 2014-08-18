package com.signalfuse.codahale.reporter;

import java.util.Map;
import com.codahale.metrics.Metric;
import com.google.common.base.Optional;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Note: This class <b>must</b> be thread safe.
 */
public interface MetricMetadata {
    public static final String SOURCE = "source";
    public static final String METRIC = "metric";
    public Map<String, String> getTags(Metric metric);
    public Optional<SignalFuseProtocolBuffers.MetricType> getMetricType(Metric metric);
    public <M extends Metric> Tagger<M> tagMetric(M metric);

    public interface Tagger<M extends Metric> {
        Tagger<M> withSourceName(String sourceName);
        Tagger<M> withMetricName(String metricName);
        Tagger<M> withMetricType(SignalFuseProtocolBuffers.MetricType metricType);
        M metric();
    }
}
