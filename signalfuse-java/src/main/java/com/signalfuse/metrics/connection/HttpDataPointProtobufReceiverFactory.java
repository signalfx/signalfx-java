package com.signalfuse.metrics.connection;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    public static final int DEFAULT_TIMEOUT_MS = 2000;
    private final DataPointReceiverEndpoint dataPointEndpoint;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;

    public HttpDataPointProtobufReceiverFactory(DataPointReceiverEndpoint dataPointEndpoint) {
        this.dataPointEndpoint = dataPointEndpoint;
    }

    public HttpDataPointProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    @Override
    public DataPointReceiver createDataPointReceiver() throws
            SignalfuseMetricsException {
        return new HttpDataPointProtobufReceiverConnection(dataPointEndpoint, this.timeoutMs);
    }
}
