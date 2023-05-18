package com.signalfx.metrics.auth;

import com.signalfx.metrics.SignalFxMetricsException;

/**
 * Thrown when {@link AuthToken} cannot find the auth token.
 */
@SuppressWarnings("serial")
public class NoAuthTokenException extends SignalFxMetricsException {
    public NoAuthTokenException(String message) {
        super(message);
    }
}
