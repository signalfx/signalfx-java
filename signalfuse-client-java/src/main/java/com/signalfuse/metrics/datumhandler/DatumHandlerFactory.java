package com.signalfuse.metrics.datumhandler;

import java.util.Set;

import com.signalfuse.metrics.auth.AuthToken;
import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.metricbuilder.errorhandler.OnSendErrorHandler;

/**
 * Factory to create DatumHandler instances given information on how they should interact
 * 
 * @author jack
 */
public interface DatumHandlerFactory {
    /**
     * Factory method to build the DatumHandler
     * 
     * @param dataPointReceiverFactory    Factory that creates endpoints to send metrics to
     * @param endpoint                    Where to send metrics
     * @param authToken                   Auth token to send on metric requests
     * @param onSendErrorHandler          Used to handle errors
     * @return Handler for these parameters out of this Factory.
     */
    DatumHandler createDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                    DataPointEndpoint endpoint,
                                    AuthToken authToken,
                                    Set<OnSendErrorHandler> onSendErrorHandler);
}
