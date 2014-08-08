package com.signalfuse.metrics.connection;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;

public class HttpDataPointProtobufReceiverFactory implements DataPointReceiverFactory {
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;

    public HttpDataPointProtobufReceiverFactory setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    @Override
    public DataPointReceiver createDataPointReceiver(DataPointReceiverEndpoint dataPointEndpoint) throws
            SignalfuseMetricsException {
        return new HttpDataPointProtobufReceiverConnection(dataPointEndpoint, this.timeoutMs);
    }
}
