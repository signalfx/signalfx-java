package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;

    public HttpDataPointProtobufReceiverFactory(SignalFxReceiverEndpoint endpoint) {
        this.endpoint = endpoint;
        this.httpClientConnectionManager = null;
    }

    public HttpDataPointProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public HttpDataPointProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    public void setHttpClientConnectionManager(
            HttpClientConnectionManager httpClientConnectionManager) {
        this.httpClientConnectionManager = httpClientConnectionManager;
    }

    @Override
    public DataPointReceiver createDataPointReceiver() throws
            SignalFxMetricsException {
        if (version == 1) {
            return new HttpDataPointProtobufReceiverConnection(
                endpoint,
                this.timeoutMs,
                buildHttpClientConnectionManager());
        } else {
            return new HttpDataPointProtobufReceiverConnectionV2(
                endpoint,
                this.timeoutMs,
                buildHttpClientConnectionManager());
        }

    }

    private HttpClientConnectionManager buildHttpClientConnectionManager() {
        if (httpClientConnectionManager != null) {
            return httpClientConnectionManager;
        } else {
            return HttpClientConnectionManagerFactory.withTimeoutMs(timeoutMs);
        }
    }
}
