package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public class HttpEventProtobufReceiverFactory implements EventReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;
    public static final int DEFAULT_MAX_RETRIES = 3;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager;
    private HttpClientConnectionManager explicitHttpClientConnectionManager;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;
    private int maxRetries = DEFAULT_MAX_RETRIES;

    public HttpEventProtobufReceiverFactory(SignalFxReceiverEndpoint endpoint) {
        this.endpoint = endpoint;
        this.httpClientConnectionManager =
            HttpClientConnectionManagerFactory.withTimeoutMs(DEFAULT_TIMEOUT_MS);
        this.explicitHttpClientConnectionManager = null;
    }

    public HttpEventProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.httpClientConnectionManager =
            HttpClientConnectionManagerFactory.withTimeoutMs(timeoutMs);
        return this;
    }

    public HttpEventProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    public HttpEventProtobufReceiverFactory setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public void setHttpClientConnectionManager(
            HttpClientConnectionManager httpClientConnectionManager) {
        this.explicitHttpClientConnectionManager = httpClientConnectionManager;
    }

    @Override
    public EventReceiver createEventReceiver() throws
            SignalFxMetricsException {
        if (version == 2) {
            return new HttpEventProtobufReceiverConnectionV2(
                endpoint,
                this.timeoutMs,
                this.maxRetries,
                resolveHttpClientConnectionManager());
        }else{
            throw new SignalFxMetricsException("Version v1 is deprecated, We encourage to use v2/event");
        }
    }

    private HttpClientConnectionManager resolveHttpClientConnectionManager() {
        if (explicitHttpClientConnectionManager != null) {
            return explicitHttpClientConnectionManager;
        }
        if (httpClientConnectionManager != null) {
            return httpClientConnectionManager;
        }
        throw new IllegalStateException("Both explicitHttpClientConnectionManager and httpClientConnectionManager are null");
    }
}
