package com.signalfuse.metrics.connection;

import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;

/**
 * A factory that creates connections to datapoint given an endpoint to connect to.
 * 
 * @author jack
 */
public interface DataPointReceiverFactory {
    /**
     * Create connection to datapoint.
     */
    DataPointReceiver createDataPointReceiver(DataPointReceiverEndpoint dataPointEndpoint)
            throws SignalfuseMetricsException;
}
