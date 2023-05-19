package com.signalfx.metrics.errorhandler;

import com.signalfx.metrics.SignalFxMetricsException;

/**
 * An error that happened trying to send a metric.
 */
public interface MetricError {
    /**
     * A code value that represents the type of error
     * @return MetricErrorType code for the error
     */
    MetricErrorType getMetricErrorType();

    /**
     * Human readable message describing the error.
     * @return Easy to read message describing the error.
     */
    String getMessage();

    /**
     * An exception, if any, that triggered this error.  Can be null!
     * @return  The exception that triggered this error, or null if no exception caused this error.
     */
    SignalFxMetricsException getException();
}
