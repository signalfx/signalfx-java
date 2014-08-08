package com.signalfuse.metrics.connection;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;

/**
 * A factory that simply returns whatever receiver was given to its constructor
 * 
 */
public class InjectedDataPointReceiverFactory implements DataPointReceiverFactory {
    private final DataPointReceiver toCall;

    public InjectedDataPointReceiverFactory(DataPointReceiver toCall) {
        this.toCall = toCall;
    }

    public DataPointReceiver createDataPointReceiver(DataPointReceiverEndpoint dataPointEndpoint)
            throws SignalfuseMetricsException {
        return toCall;
    }
}
