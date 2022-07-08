package com.signalfx.connection;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

public class RetryStrategy implements ServiceUnavailableRetryStrategy {
    private final int maxRetries;

    public RetryStrategy(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean retryRequest(final HttpResponse httpResponse, final int executionCount, final HttpContext httpContext) {
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        return executionCount <= maxRetries && (statusCode == HttpStatus.SC_REQUEST_TIMEOUT || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT || statusCode == 598 || statusCode == -1);
    }

    @Override
    public long getRetryInterval() {
        return 0;
    }
}
