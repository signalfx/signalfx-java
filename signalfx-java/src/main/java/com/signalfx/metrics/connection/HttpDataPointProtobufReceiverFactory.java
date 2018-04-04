package com.signalfx.metrics.connection;

import javax.net.ssl.SSLContext;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

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

        // Same as new BasicHttpClientConnectionManager() but with STRICT_HOSTNAME_VERIFIER.
        this.httpClientConnectionManager = new BasicHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(SSLContexts.createDefault(),
                            SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER))
                    .build());
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
            return new HttpDataPointProtobufReceiverConnection(endpoint, this.timeoutMs, httpClientConnectionManager);
        } else {
            return new HttpDataPointProtobufReceiverConnectionV2(endpoint, this.timeoutMs, httpClientConnectionManager);
        }

    }
}
