package com.signalfuse.metrics.auth;

import com.signalfuse.metrics.SignalfuseMetricsException;

@SuppressWarnings("serial")
public class NoAuthTokenException extends SignalfuseMetricsException {
    public NoAuthTokenException(String message) {
        super(message);
    }
}
