package com.signalfx.metrics.connection;

/**
 * A factory that creates connections to event given an endpoint to connect to.
 *
 * @author jack
 */
public interface EventReceiverFactory {
    /**
     * @return A newly cleated event receiver.
     */
    EventReceiver createEventReceiver();
}
