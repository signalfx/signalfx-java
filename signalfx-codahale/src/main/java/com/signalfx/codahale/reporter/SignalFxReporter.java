/**
* Copyright (C) 2015 SignalFx, Inc.
*/
package com.signalfx.codahale.reporter;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SourceNameHelper;
import com.signalfx.metrics.auth.AuthToken;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.DataPointReceiverFactory;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Reporter object for codahale metrics that reports values to com.signalfx.signalfx at some
 * interval.
 */
public class SignalFxReporter extends ScheduledReporter {
    private final AggregateMetricSender aggregateMetricSender;
    private final Set<MetricDetails> detailsToAdd;
    private final MetricMetadata metricMetadata;
    private final boolean useLocalTime;
    private final ImmutableMap<String, DimensionInclusion> defaultDimensions;

    protected SignalFxReporter(MetricRegistry registry, String name, MetricFilter filter,
                                 TimeUnit rateUnit, TimeUnit durationUnit,
                                 AggregateMetricSender aggregateMetricSender,
                                 Set<MetricDetails> detailsToAdd,
                                 MetricMetadata metricMetadata) {
        this(registry, name, filter, rateUnit, durationUnit, aggregateMetricSender, detailsToAdd,
                metricMetadata, false, Collections.<String, DimensionInclusion> emptyMap());
    }

    public SignalFxReporter(MetricRegistry registry, String name, MetricFilter filter,
                              TimeUnit rateUnit, TimeUnit durationUnit,
                              AggregateMetricSender aggregateMetricSender,
                              Set<MetricDetails> detailsToAdd, MetricMetadata metricMetadata,
                              boolean useLocalTime, Map<String, DimensionInclusion> defaultDimensions) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.aggregateMetricSender = aggregateMetricSender;
        this.useLocalTime = useLocalTime;
        this.detailsToAdd = detailsToAdd;
        this.metricMetadata = metricMetadata;
        this.defaultDimensions = ImmutableMap.copyOf(defaultDimensions);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        AggregateMetricSenderSessionWrapper session = new AggregateMetricSenderSessionWrapper(
                aggregateMetricSender.createSession(), Collections.unmodifiableSet(detailsToAdd), metricMetadata,
                aggregateMetricSender.getDefaultSourceName(), "sf_source", useLocalTime, defaultDimensions);

        try {
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                session.addMetric(entry.getValue(), entry.getKey(),
                        SignalFxProtocolBuffers.MetricType.GAUGE, entry.getValue().getValue());
            }
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                if (entry.getValue() instanceof IncrementalCounter) {
                    session.addMetric(entry.getValue(), entry.getKey(),
                            SignalFxProtocolBuffers.MetricType.COUNTER,
                            ((IncrementalCounter)entry.getValue()).getCountChange());
                } else {
                    session.addMetric(entry.getValue(), entry.getKey(),
                            SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER,
                            entry.getValue().getCount());
                }
            }
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                session.addHistogram(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                session.addMetered(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                session.addTimer(entry.getKey(), entry.getValue());
            }
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                // Unable to register... these exceptions handled by AggregateMetricSender
            }
        }
    }

    public MetricMetadata getMetricMetadata() {
        return metricMetadata;
    }

    public enum MetricDetails {
        // For {@link com.codahale.metrics.Sampling}
        MEDIAN("median"),
        PERCENT_75("75th"),
        PERCENT_95("95th"),
        PERCENT_98("98th"),
        PERCENT_99("99th"),
        PERCENT_999("999th"),
        MAX("max"),
        MIN("min"),
        STD_DEV("stddev"),
        MEAN("mean"),

        // For {@link com.codahale.metrics.Counting}
        COUNT("count"),

        // For {@link com.codahale.metrics.Metered}
        RATE_MEAN("rate.mean"),
        RATE_1_MIN("rate.1min"),
        RATE_5_MIN("rate.5min"),
        RATE_15_MIN("rate.15min");
        public static final Set<MetricDetails> ALL = Collections.unmodifiableSet(EnumSet.allOf(MetricDetails.class));
        public static final Set<MetricDetails> DEFAULTS = ImmutableSet.of(COUNT, MIN, MEAN, MAX);

        private final String description;

        MetricDetails(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class Builder {
        private final MetricRegistry registry;
        private String defaultSourceName;
        private AuthToken authToken;
        private SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint();
        private String name = "signalfx-reporter";
        private int timeoutMs = HttpDataPointProtobufReceiverFactory.DEFAULT_TIMEOUT_MS;
        private DataPointReceiverFactory dataPointReceiverFactory = new
                HttpDataPointProtobufReceiverFactory(endpoint);
        private MetricFilter filter = MetricFilter.ALL;
        private TimeUnit rateUnit = TimeUnit.SECONDS;
        private TimeUnit durationUnit = TimeUnit.MILLISECONDS; // Maybe nano eventually?
        private Set<MetricDetails> detailsToAdd = MetricDetails.DEFAULTS;
        private Collection<OnSendErrorHandler> onSendErrorHandlerCollection = Collections.emptyList();
        private MetricMetadata metricMetadata = new MetricMetadataImpl();
        private boolean useLocalTime = false;
        private final ImmutableMap.Builder<String, DimensionInclusion> defaultDimensions = new ImmutableMap.Builder<String, DimensionInclusion>();

        public Builder(MetricRegistry registry, String authToken) {
            this(registry, new StaticAuthToken(authToken));
        }

        public Builder(MetricRegistry registry, AuthToken authToken) {
            this(registry, authToken, SourceNameHelper.getDefaultSourceName());
        }

        public Builder(MetricRegistry registry, AuthToken authToken, String defaultSourceName) {
            this.registry = registry;
            this.authToken = authToken;
            this.defaultSourceName = requireNonNull(defaultSourceName, "defaultSourceName must be a non-null value");
        }

        public Builder setDefaultSourceName(String defaultSourceName) {
            this.defaultSourceName = requireNonNull(defaultSourceName, "defaultSourceName must be a non-null value");
            return this;
        }

        public Builder setAuthToken(AuthToken authToken) {
            this.authToken = authToken;
            return this;
        }

        public Builder setEndpoint(SignalFxReceiverEndpoint endpoint) {
            this.endpoint = endpoint;
            this.dataPointReceiverFactory =
                    new HttpDataPointProtobufReceiverFactory(endpoint)
                            .setTimeoutMs(this.timeoutMs);
            return this;
        }

        @Deprecated
        public Builder setDataPointEndpoint(DataPointReceiverEndpoint dataPointEndpoint) {
            return setEndpoint(dataPointEndpoint);
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            this.dataPointReceiverFactory =
                    new HttpDataPointProtobufReceiverFactory(endpoint)
                            .setTimeoutMs(this.timeoutMs);
            return this;
        }

        @Deprecated
        public Builder setVersion(int version) {
            return this;
        }

        public Builder setDataPointReceiverFactory(
                DataPointReceiverFactory dataPointReceiverFactory) {
            this.dataPointReceiverFactory = dataPointReceiverFactory;
            return this;
        }

        public Builder setFilter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setRateUnit(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder setDurationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder setDetailsToAdd(Set<MetricDetails> detailsToAdd) {
            this.detailsToAdd = detailsToAdd;
            return this;
        }

        public Builder setOnSendErrorHandlerCollection(
                Collection<OnSendErrorHandler> onSendErrorHandlerCollection) {
            this.onSendErrorHandlerCollection = onSendErrorHandlerCollection;
            return this;
        }

        public Builder setMetricMetadata(MetricMetadata metricMetadata) {
            this.metricMetadata = metricMetadata;
            return this;
        }

        /**
         * Will use the local system time, rather than zero, on sent datapoints.
         * @param useLocalTime    If true, use local system time
         * @return this
         */
        public Builder useLocalTime(boolean useLocalTime) {
            this.useLocalTime = useLocalTime;
            return this;
        }

        /**
         * Adds all dimensions to the default dimensions to be sent with every datapoint from this
         * reporter. This means they will be added to distributed counters (as opposed to cumulative counters),
         * which means that if there is any dimension in this map which is unique to the emitter (i.e a hostname)
         * then the distributed counter will not aggregate as expected. For dimension that are unique to each emitter use
         * either {@link #addUniqueDimension(String, String)} or {@link #addUniqueDimensions(Map)}
         *
         * @param dimensions
         *            non-null map of string value pairs
         * @return this
         */
        public Builder addDimensions(Map<String, String> dimensions) {
            // loop here to get "null value" protection of addDimension
            for (Map.Entry<String, String> entry: dimensions.entrySet()) {
                this.addDimension(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Adds a dimension to the default dimensions to be sent with every datapoint from this
         * reporter.  This means they will be added to distributed counters (as opposed to cumulative counters),
         * which means that if the name and value is unique to the emitter (i.e a hostname)
         * then the distributed counter will not aggregate as expected. For dimension that are unique to each emitter use
         * {@link #addUniqueDimension(String, String)}
         *
         * @param name
         *            Name of the dimension
         * @param value
         *            Value of the dimension. If null then the dimension is not added.
         * @return this
         */
        public Builder addDimension(String name, String value) {
            if (value != null) {
                this.defaultDimensions.put(name, DimensionInclusion.shared(value));
            }
            return this;
        }

        /**
         * Adds all dimensions to the default dimensions to be sent with every datapoint that is not a distributed counter.
         * This method should be used for adding dimensions which are unique to the emitter (such as host name), for shared
         * dimensions which can be sent with distributed counters use either {@link #addDimension(String, String)} or {@link #addDimensions(Map)}
         * @param dimensions
         *            non-null map of string value pairs
         * @return this
         */
        public Builder addUniqueDimensions(Map<String, String> dimensions) {
            // loop here to get "null value" protection of addDimension
            for (Map.Entry<String, String> entry: dimensions.entrySet()) {
                this.addUniqueDimension(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Adds a dimension to the default dimensions to be sent with every datapoint that is not a distributed counter.
         * This method should be used for adding dimensions which are unique to the emitter (such as host name), for shared
         * dimensions use {@link #addDimension(String, String)}
         *
         * @param name
         *            Name of the dimension
         * @param value
         *            Value of the dimension. If null then the dimension is not added.
         * @return this
         */
        public Builder addUniqueDimension(String name, String value) {
            if (value != null) {
                this.defaultDimensions.put(name, DimensionInclusion.unique(value));
            }
            return this;
        }

        public SignalFxReporter build() {
            AggregateMetricSender aggregateMetricSender = new AggregateMetricSender(
                    defaultSourceName, dataPointReceiverFactory, authToken,
                    onSendErrorHandlerCollection);
            return new SignalFxReporter(registry, name, filter, rateUnit, durationUnit,
                    aggregateMetricSender, detailsToAdd, metricMetadata, useLocalTime,
                    defaultDimensions.build());
        }
    }
}
