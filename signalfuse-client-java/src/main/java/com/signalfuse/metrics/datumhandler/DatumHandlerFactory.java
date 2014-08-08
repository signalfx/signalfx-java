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
     * @param dataPointReceiverFactory
     * @param endpoint
     * @param authToken
     * @param onSendErrorHandler
     * @return
     */
    DatumHandler createDatumHandler(DataPointReceiverFactory dataPointReceiverFactory,
                                    DataPointEndpoint endpoint,
                                    AuthToken authToken,
                                    Set<OnSendErrorHandler> onSendErrorHandler);
}
