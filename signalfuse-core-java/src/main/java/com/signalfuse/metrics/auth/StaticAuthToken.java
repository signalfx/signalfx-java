package com.signalfuse.metrics.auth;

/**
 * Uses a static auth token
 * 
 * @author jack
 */
public class StaticAuthToken implements AuthToken {
    private final String authToken;

    public StaticAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String getAuthToken() {
        return authToken;
    }
}
