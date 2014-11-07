package com.signalfuse.metrics.connection;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_VERSION = 1;

    private final DataPointReceiverEndpoint dataPointEndpoint;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;
    private int version = DEFAULT_VERSION;

    public HttpDataPointProtobufReceiverFactory(DataPointReceiverEndpoint dataPointEndpoint) {
        this.dataPointEndpoint = dataPointEndpoint;
    }

    public HttpDataPointProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public HttpDataPointProtobufReceiverFactory setVersion(int version) {
        this.version = version;
        return this;
    }

    @Override
    public DataPointReceiver createDataPointReceiver() throws
            SignalfuseMetricsException {
        if (version == 1) {
            return new HttpDataPointProtobufReceiverConnection(dataPointEndpoint, this.timeoutMs);
        } else {
            return new HttpDataPointProtobufReceiverConnectionV2(dataPointEndpoint, this.timeoutMs);
        }

    }
}
