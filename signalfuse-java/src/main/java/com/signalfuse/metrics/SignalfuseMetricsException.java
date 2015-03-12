package com.signalfx.metrics;

public class SignalfuseMetricsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SignalfuseMetricsException() {
    }

    public SignalfuseMetricsException(String message) {
        super(message);
    }

    public SignalfuseMetricsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignalfuseMetricsException(Throwable cause) {
        super(cause);
    }
}
