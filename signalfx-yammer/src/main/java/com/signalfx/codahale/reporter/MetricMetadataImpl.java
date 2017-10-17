package com.signalfx.codahale.reporter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.yammer.metrics.core.Metric;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 *
 * Implementation of MetricMetadata
 *
 */

public class MetricMetadataImpl implements MetricMetadata {
    private final ConcurrentMap<Metric, Metadata> metaDataCollection;

    public MetricMetadataImpl() {
        // This map must be thread safe
        metaDataCollection = new ConcurrentHashMap<Metric, Metadata>();
    }

    @Override
    public Map<String, String> getTags(Metric metric) {
        Metadata existingMetaData = metaDataCollection.get(metric);
        if (existingMetaData == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap(existingMetaData.tags);
        }
    }

    @Override
    public Optional<SignalFxProtocolBuffers.MetricType> getMetricType(Metric metric) {
        Metadata existingMetaData = metaDataCollection.get(metric);
        if (existingMetaData == null || existingMetaData.metricType == null) {
            return Optional.absent();
        } else {
            return Optional.of(existingMetaData.metricType);
        }
    }

    @Override
    public <M extends Metric> Tagger<M> tagMetric(M metric) {
        return forMetric(metric);
    }

    @Override
    public <M extends Metric> Tagger<M> forMetric(M metric) {
        Metadata metadata = metaDataCollection.get(metric);
        if (metadata == null) {
            synchronized (this) {
                if (metadata == null) {
                    metadata = new Metadata();
                    Metadata oldMetaData = metaDataCollection.put(metric, metadata);
                    Preconditions.checkArgument(oldMetaData == null,
                            "Concurrency issue adding metadata");
                }
            }
        }
        return new TaggerImpl<M>(metric, metadata);
    }

    @Override
    public <M extends Metric> boolean removeMetric(M metric) {
        return metaDataCollection.remove(metric) != null;
    }

    private static abstract class TaggerBaseImpl<M extends Metric, T extends TaggerBase<M, T>>
            implements TaggerBase<M, T>{
        protected final Metadata thisMetricsMetadata;

        @Override
        public T withDimension(String key, String value) {
            thisMetricsMetadata.tags.put(key, value);
            return (T) this;
        }

        TaggerBaseImpl(Metadata thisMetricsMetadata) {
            this.thisMetricsMetadata = thisMetricsMetadata;
        }

        @Override
        public T withSourceName(String sourceName) {
            thisMetricsMetadata.tags.put(SOURCE, sourceName);
            return (T) this;
        }

        @Override
        public T withMetricName(String metricName) {
            thisMetricsMetadata.tags.put(METRIC, metricName);
            return (T) this;
        }

        @Override
        public T withMetricType(
                SignalFxProtocolBuffers.MetricType metricType) {
            thisMetricsMetadata.metricType = metricType;
            return (T) this;
        }

    }

    private static final class TaggerImpl<M extends Metric> extends TaggerBaseImpl<M, Tagger<M>>
            implements Tagger<M> {
        private final M metric;

        TaggerImpl(M metric, Metadata thisMetricsMetadata) {
            super(thisMetricsMetadata);
            this.metric = metric;
        }

        @Override public M metric() {
            return metric;
        }
    }

    private static final class Metadata {
        private final Map<String, String> tags;
        private SignalFxProtocolBuffers.MetricType metricType;

        private Metadata() {
            tags = new ConcurrentHashMap<String, String>(6);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Metadata)) {
                return false;
            }

            Metadata metadata = (Metadata) o;

            if (metricType != metadata.metricType) {
                return false;
            }
            if (tags != null ? !tags.equals(metadata.tags) : metadata.tags != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = tags != null ? tags.hashCode() : 0;
            result = 31 * result + (metricType != null ? metricType.hashCode() : 0);
            return result;
        }
    }

}
