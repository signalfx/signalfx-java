package com.signalfuse.metrics.dropwizard;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.signalfuse.codahale.metrics.SignalFuseReporter;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;

import io.dropwizard.metrics.BaseReporterFactory;

@JsonTypeName("signalfuse")
public class SignalFuseReporterFactory extends BaseReporterFactory {
    @NotNull
    @JsonProperty
    private String authToken;

    @JsonProperty
    private String defaultSourceName;

    @JsonProperty
    private String signalfuseUrlScheme;

    @JsonProperty
    private String signalfuseHostname;

    @JsonProperty
    private Integer signalfusePort;

    @JsonProperty
    private String name;

    @JsonProperty
    private Set<SignalFuseReporter.MetricDetails> metricDetailsSet;

    @JsonProperty
    private Integer timeoutMs;

    public ScheduledReporter build(MetricRegistry metricRegistry) {
        SignalFuseReporter.Builder builder =
                new SignalFuseReporter.Builder(metricRegistry, authToken)
                .setRateUnit(getRateUnit())
                .setDurationUnit(getDurationUnit())
                .setFilter(getFilter());
        if (defaultSourceName != null) {
            builder.setDefaultSourceName(defaultSourceName);
        }
        String scheme = DataPointEndpoint.DEFAULT_SCHEME;
        String hostname = DataPointEndpoint.DEFAULT_HOSTNAME;
        int defaultPort = DataPointEndpoint.DEFAULT_PORT;
        if (signalfuseUrlScheme != null) {
            scheme = signalfuseUrlScheme;
        }
        if (signalfuseHostname != null) {
            hostname = signalfuseHostname;
        }
        if (signalfusePort != null) {
            defaultPort = signalfusePort;
        }
        builder.setDataPointEndpoint(new DataPointEndpoint(scheme, hostname, defaultPort));
        if (name != null) {
            builder.setName(name);
        }
        if (metricDetailsSet != null) {
            builder.setDetailsToAdd(metricDetailsSet);
        }
        if (timeoutMs != null) {
            builder.setTimeoutMs(timeoutMs);
        }
        return builder.build();
    }
}
