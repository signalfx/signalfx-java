package com.signalfx.metrics.auth;

import com.signalfx.metrics.SignalFxMetricsException;

/**
 * Thrown when {@link com.signalfx.metrics.auth.AuthToken} cannot find the auth token.
 */
@SuppressWarnings("serial")
public class NoAuthTokenException extends SignalFxMetricsException {
    public NoAuthTokenException(String message) {
        super(message);
    }
}
