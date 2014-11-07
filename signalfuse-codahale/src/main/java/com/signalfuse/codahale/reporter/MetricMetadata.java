package com.signalfuse.codahale.reporter;

import java.util.Map;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.signalfuse.codahale.metrics.MetricBuilder;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

/**
 * Allows users to modify a metric with different source or metric parts than the default we pick
 * from codahale.  Note: This class <b>must</b> be thread safe.
 */
public interface MetricMetadata {
    public static final String SOURCE = "source";
    public static final String METRIC = "metric";
    public Map<String, String> getTags(Metric metric);
    public Optional<SignalFuseProtocolBuffers.MetricType> getMetricType(Metric metric);

    /**
     * Create an object to tag a metric with data.  Registering two different metrics with the same
     * metadata will result in an exception.  In that case, use {@link #forBuilder(com.signalfuse.codahale.metrics.MetricBuilder)}
     * @param metric    The metric will tag.
     * @param <M>       The type of metric.  It is implied by the metric type.
     * @return An object to tag the given metric.
     */
    public <M extends Metric> Tagger<M> forMetric(M metric);

    @Deprecated
    public <M extends Metric> Tagger<M> tagMetric(M metric);

    /**
     * Create a tagger for a type of objects.  This is different than {@link #forMetric(com.codahale.metrics.Metric)}
     * because it will not use the builder to create a metric unless if it already exists.
     * @param metricBuilder    The builder that creates metrics.
     * @param <M>              The type of metric to create.
     * @return An object to tag metrics.
     */
    public <M extends Metric> BuilderTagger<M> forBuilder(MetricBuilder<M> metricBuilder);

    public interface TaggerBase<M extends Metric, T extends TaggerBase<M, ?>> {
        T withSourceName(String sourceName);
        T withMetricName(String metricName);
        T withDimension(String key, String value);
        T withMetricType(SignalFuseProtocolBuffers.MetricType metricType);
    }

    public interface Tagger<M extends Metric> extends TaggerBase<M, Tagger<M>> {
        /**
         * Helper that registers a metric in the registry, throwing an exception if it already
         * exists.
         * @param metricRegistry    Where to register the metric.
         * @return The new metric
         */
        M register(MetricRegistry metricRegistry);
        M metric();
    }

    public interface BuilderTagger<M extends Metric> extends TaggerBase<M, BuilderTagger<M>> {
        /**
         * Create this metric in the registry, or return the currently registered metric if it
         * already exists.
         * @param metricRegistry    Registry to create the metric in
         * @return The new (or existing) metric.
         */
        M createOrGet(MetricRegistry metricRegistry);
    }
}
