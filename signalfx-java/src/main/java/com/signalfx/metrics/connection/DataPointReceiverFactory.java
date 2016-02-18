package com.signalfx.metrics.connection;

/**
 * A factory that creates connections to datapoint given an endpoint to connect to.
 * 
 * @author jack
 */
public interface DataPointEventReceiverFactory {
    /**
     * Create connection to datapoint.
     */
    DataPointEventReceiver createDataPointEventReceiver();
}
