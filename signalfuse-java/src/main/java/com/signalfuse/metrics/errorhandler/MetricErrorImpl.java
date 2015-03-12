package com.signalfx.metrics.errorhandler;

import com.signalfx.metrics.SignalFxMetricsException;

public class MetricErrorImpl implements MetricError {
    private final String message;
    private final MetricErrorType code;
    private final SignalFxMetricsException signalfxMetricsException;

    public MetricErrorImpl(String message, MetricErrorType code, SignalFxMetricsException signalfxMetricsException) {
        this.message = message;
        this.code = code;
        this.signalfxMetricsException = signalfxMetricsException;
    }

    @Override public MetricErrorType getMetricErrorType() {
        return this.code;
    }

    @Override public String getMessage() {
        return this.message;
    }

    @Override public SignalFxMetricsException getException() {
        return this.signalfxMetricsException;
    }
}
