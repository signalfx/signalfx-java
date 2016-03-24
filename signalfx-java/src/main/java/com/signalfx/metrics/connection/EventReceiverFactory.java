package com.signalfx.metrics.connection;

/**
 * A factory that creates connections to event given an endpoint to connect to.
 *
 */
public interface EventReceiverFactory {
    /**
     * Create connection to event.
     */
    EventReceiver createEventReceiver();
}
