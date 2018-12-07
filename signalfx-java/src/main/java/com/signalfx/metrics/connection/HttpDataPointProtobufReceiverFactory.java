package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;

import com.google.common.base.MoreObjects;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager;
    private HttpClientConnectionManager explicitHttpClientConnectionManager;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;

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
                resolveHttpClientConnectionManager());
        } else {
            return new HttpDataPointProtobufReceiverConnectionV2(
                endpoint,
                this.timeoutMs,
                resolveHttpClientConnectionManager());
        }

    }

    private HttpClientConnectionManager resolveHttpClientConnectionManager() {
        return MoreObjects.firstNonNull(explicitHttpClientConnectionManager, httpClientConnectionManager);
    }
}
