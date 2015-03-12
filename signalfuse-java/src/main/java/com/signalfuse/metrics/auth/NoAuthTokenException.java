package com.signalfx.metrics.auth;

import com.signalfx.metrics.SignalfuseMetricsException;

/**
 * Thrown when {@link com.signalfx.metrics.auth.AuthToken} cannot find the auth token.
 */
@SuppressWarnings("serial")
public class NoAuthTokenException extends SignalfuseMetricsException {
    public NoAuthTokenException(String message) {
        super(message);
    }
}
