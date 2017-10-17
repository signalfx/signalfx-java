package com.signalfx.codahale.reporter;

import java.util.Map;
import com.yammer.metrics.core.Metric;
import com.google.common.base.Optional;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Allows users to modify a metric with different source or metric parts than the default we pick
 * from codahale.  Note: This class <b>must</b> be thread safe.
 */
public interface MetricMetadata {
    public static final String SOURCE = "source";
    public static final String METRIC = "metric";
    public Map<String, String> getTags(Metric metric);
    public Optional<SignalFxProtocolBuffers.MetricType> getMetricType(Metric metric);

    /**
     * Create an object to tag a metric with data.  Registering two different metrics with the same
     * metadata will result in an exception.
     * @param metric    The metric will tag.
     * @param <M>       The type of metric.  It is implied by the metric type.
     * @return An object to tag the given metric.
     */
    public <M extends Metric> Tagger<M> forMetric(M metric);

    @Deprecated
    public <M extends Metric> Tagger<M> tagMetric(M metric);

    /**
     * Removes the specified metric from the metric metadata.
     * @param metric           The metric to remove, cannot be null.
     * @return True if the metric was found and removed, false otherwise
     */
    public <M extends Metric> boolean removeMetric(M metric);

    public interface TaggerBase<M extends Metric, T extends TaggerBase<M, ?>> {
        /**
         *  Tag the metric with a sf_source
         * @param sourceName    Source name for the sf_source
         * @return this
         * @deprecated The use of the build in source parameter is deprecated and discouraged.  Use
         *             {@link #withDimension(String, String)} instead.
         */
        @Deprecated
        T withSourceName(String sourceName);

        /**
         * Changes the metric name of this metric from the default (which is the codahale metric
         * name), to another string
         * @param metricName    The new name in SignalFx of this metric
         * @return this
         */
        T withMetricName(String metricName);

        /**
         * Adds a dimension to this metric
         * @param key      The dimension key to add
         * @param value    The dimensino value to add
         * @return this
         */
        T withDimension(String key, String value);

        /**
         * Changes the default metric type of this metric to the SignalFx metric type passed in
         * @param metricType    The new metric type of this metric
         * @return this
         */
        T withMetricType(SignalFxProtocolBuffers.MetricType metricType);
    }

    public interface Tagger<M extends Metric> extends TaggerBase<M, Tagger<M>> {

        /**
         * Returns the tagged metric
         * @return the tagged metric
         */
        M metric();
    }

}
