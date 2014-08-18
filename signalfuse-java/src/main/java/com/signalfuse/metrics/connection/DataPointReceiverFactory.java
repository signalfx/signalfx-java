package com.signalfuse.metrics.connection;

/**
 * A factory that creates connections to datapoint given an endpoint to connect to.
 * 
 * @author jack
 */
public interface DataPointReceiverFactory {
    /**
     * Create connection to datapoint.
     */
    DataPointReceiver createDataPointReceiver();
}
