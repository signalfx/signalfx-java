package com.signalfx.metrics.connection;

/**
 * A factory that creates connections to datapoint given an endpoint to connect to.
 *
 * @author jack
 */
public interface DataPointReceiverFactory {
    /**
     * @return A newly created datapoint receiver.
     */
    DataPointReceiver createDataPointReceiver();
}
