package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public class HttpProtobufReceiverFactory implements DataPointEventReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager =
            new BasicHttpClientConnectionManager();
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;

    public HttpProtobufReceiverFactory (SignalFxReceiverEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public HttpProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public HttpProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    public void setHttpClientConnectionManager(
            HttpClientConnectionManager httpClientConnectionManager) {
        this.httpClientConnectionManager = httpClientConnectionManager;
    }

    @Override
    public DataPointEventReceiver createDataPointEventReceiver() throws
            SignalFxMetricsException {
        if (version == 1) {
            return new HttpProtobufReceiverConnection(endpoint, this.timeoutMs, httpClientConnectionManager);
        } else {
            return new HttpProtobufReceiverConnectionV2(endpoint, this.timeoutMs, httpClientConnectionManager);
        }

    }
}
