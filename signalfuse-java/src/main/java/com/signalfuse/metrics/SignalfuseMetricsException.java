package com.signalfx.metrics;

public class SignalFxMetricsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SignalFxMetricsException() {
    }

    public SignalFxMetricsException(String message) {
        super(message);
    }

    public SignalFxMetricsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignalFxMetricsException(Throwable cause) {
        super(cause);
    }
}
