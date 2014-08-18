package com.signalfuse.codahale.reporter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.codahale.metrics.Metric;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public class MetricMetadataImpl implements MetricMetadata {
    private final Map<Metric, Metadata> metaDataCollection;

    public MetricMetadataImpl() {
        // This map must be thread safe
        metaDataCollection = new ConcurrentHashMap<Metric, Metadata>();
    }

    public Map<String, String> getTags(Metric metric) {
        Metadata existingMetaData = metaDataCollection.get(metric);
        if (existingMetaData == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap(existingMetaData.tags);
        }
    }

    @Override
    public Optional<SignalFuseProtocolBuffers.MetricType> getMetricType(Metric metric) {
        Metadata existingMetaData = metaDataCollection.get(metric);
        if (existingMetaData == null) {
            return Optional.absent();
        } else {
            return Optional.of(existingMetaData.metricType);
        }
    }

    @Override
    public synchronized <M extends Metric> Tagger<M> tagMetric(M metric) {
        if (metaDataCollection.containsKey(metric)) {
            return new TaggerImpl<M>(metric, metaDataCollection.get(metric));
        } else {
            Metadata thisMetricsMetadata = new Metadata();
            Metadata oldMetaData = metaDataCollection.put(metric, thisMetricsMetadata);
            Preconditions.checkArgument(oldMetaData == null, "Concurrency issue adding metadat");
            return new TaggerImpl<M>(metric, thisMetricsMetadata);
        }
    }

    private static final class TaggerImpl<M extends Metric> implements Tagger<M> {

        private final M metric;
        private final Metadata thisMetricsMetadata;

        TaggerImpl(M metric, Metadata thisMetricsMetadata) {
            this.metric = metric;
            this.thisMetricsMetadata = thisMetricsMetadata;
        }

        @Override public Tagger<M> withSourceName(String sourceName) {
            thisMetricsMetadata.tags.put(SOURCE, sourceName);
            return this;
        }

        @Override public Tagger<M> withMetricName(String metricName) {
            thisMetricsMetadata.tags.put(METRIC, metricName);
            return this;
        }

        @Override public Tagger<M> withMetricType(
                SignalFuseProtocolBuffers.MetricType metricType) {
            thisMetricsMetadata.metricType = metricType;
            return this;
        }

        @Override public M metric() {
            return metric;
        }
    }

    private static final class Metadata {
        private final Map<String, String> tags;
        private SignalFuseProtocolBuffers.MetricType metricType = null;

        private Metadata() {
            tags = new ConcurrentHashMap<String, String>(6);
        }
    }
}
