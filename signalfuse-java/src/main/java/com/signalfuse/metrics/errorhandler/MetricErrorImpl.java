package com.signalfuse.metrics.errorhandler;

import com.signalfuse.metrics.SignalfuseMetricsException;

public class MetricErrorImpl implements MetricError {
    private final String message;
    private final MetricErrorType code;
    private final SignalfuseMetricsException signalfuseMetricsException;

    public MetricErrorImpl(String message, MetricErrorType code, SignalfuseMetricsException signalfuseMetricsException) {
        this.message = message;
        this.code = code;
        this.signalfuseMetricsException = signalfuseMetricsException;
    }

    @Override public MetricErrorType getMetricErrorType() {
        return this.code;
    }

    @Override public String getMessage() {
        return this.message;
    }

    @Override public SignalfuseMetricsException getException() {
        return this.signalfuseMetricsException;
    }
}
