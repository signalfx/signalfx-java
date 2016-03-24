package com.signalfx.metrics.connection;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public class HttpEventProtobufReceiverFactory implements EventReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 2;

    private final SignalFxReceiverEndpoint endpoint;
    private HttpClientConnectionManager httpClientConnectionManager =
            new BasicHttpClientConnectionManager();
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;

    public HttpEventProtobufReceiverFactory(SignalFxReceiverEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public HttpEventProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public HttpEventProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    public void setHttpClientConnectionManager(
            HttpClientConnectionManager httpClientConnectionManager) {
        this.httpClientConnectionManager = httpClientConnectionManager;
    }

    @Override
    public EventReceiver createEventReceiver() throws
            SignalFxMetricsException {
        if (version == 2) {
            return new HttpEventProtobufReceiverConnectionV2(endpoint, this.timeoutMs, httpClientConnectionManager);
        }else{
            throw new SignalFxMetricsException("Version v1 is deprecated, We encourage to use v2/event");
        }
    }
}
