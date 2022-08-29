package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;

import com.google.common.base.MoreObjects;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

import java.io.IOException;
import java.util.List;

import static com.signalfx.connection.RetryDefaults.DEFAULT_MAX_RETRIES;
import static com.signalfx.connection.RetryDefaults.DEFAULT_NON_RETRYABLE_EXCEPTIONS;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager;
    private HttpClientConnectionManager explicitHttpClientConnectionManager;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private List<Class<? extends IOException>> nonRetryableExceptions = DEFAULT_NON_RETRYABLE_EXCEPTIONS;

    public HttpDataPointProtobufReceiverFactory(SignalFxReceiverEndpoint endpoint) {
        this.endpoint = endpoint;
        this.httpClientConnectionManager =
            HttpClientConnectionManagerFactory.withTimeoutMs(DEFAULT_TIMEOUT_MS);
        this.explicitHttpClientConnectionManager = null;
    }

    public HttpDataPointProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.httpClientConnectionManager =
            HttpClientConnectionManagerFactory.withTimeoutMs(timeoutMs);
        return this;
    }

    public HttpDataPointProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    public HttpDataPointProtobufReceiverFactory setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public HttpDataPointProtobufReceiverFactory setNonRetryableExceptions(List<Class<? extends IOException>> clazzes) {
        this.nonRetryableExceptions = clazzes;
        return this;
    }

    public void setHttpClientConnectionManager(
            HttpClientConnectionManager httpClientConnectionManager) {
        this.explicitHttpClientConnectionManager = httpClientConnectionManager;
    }

    @Override
    public DataPointReceiver createDataPointReceiver() throws
            SignalFxMetricsException {
        if (version == 1) {
            return new HttpDataPointProtobufReceiverConnection(
                endpoint,
                this.timeoutMs,
                this.maxRetries,
                resolveHttpClientConnectionManager(),
                this.nonRetryableExceptions);
        } else {
            return new HttpDataPointProtobufReceiverConnectionV2(
                endpoint,
                this.timeoutMs,
                this.maxRetries,
                resolveHttpClientConnectionManager(),
                this.nonRetryableExceptions);
        }

    }

    private HttpClientConnectionManager resolveHttpClientConnectionManager() {
        return MoreObjects.firstNonNull(explicitHttpClientConnectionManager, httpClientConnectionManager);
    }
}
