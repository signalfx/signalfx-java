package com.signalfx.metrics.auth;

/**
 * Identifies how to find the auth token during datapoint requests
 * 
 * @author jack
 */
public interface AuthToken {
    String getAuthToken() throws NoAuthTokenException;
}
