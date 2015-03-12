package com.signalfx.metrics.errorhandler;

/**
 * Handler that calls exit() on any failure at all
 * 
 * @author jack
 */
public class ExitOnSendError implements OnSendErrorHandler {
    @Override
    public void handleError(MetricError metricError) {
        System.exit(1);
    }
}
