package com.signalfx.metrics.errorhandler;

/**
 * Listener interface to handle failures to send a metric
 * 
 * @author jack
 */
public interface OnSendErrorHandler {
    void handleError(MetricError metricError);
}
