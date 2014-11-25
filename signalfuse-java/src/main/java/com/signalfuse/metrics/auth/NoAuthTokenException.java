package com.signalfuse.metrics.auth;

import com.signalfuse.metrics.SignalfuseMetricsException;

/**
 * Thrown when {@link com.signalfuse.metrics.auth.AuthToken} cannot find the auth token.
 */
@SuppressWarnings("serial")
public class NoAuthTokenException extends SignalfuseMetricsException {
    public NoAuthTokenException(String message) {
        super(message);
    }
}
