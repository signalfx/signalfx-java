package com.signalfuse.metrics.metric;

/**
 * A metric object is a handle to an individual metric - either a gauge or a counter.
 * 
 * @author kris
 */
public interface Metric {

    /**
     * Get the name of the source of this metric.
     * 
     * @return Metric's source name
     */
    String getSource();

    /**
     * Get the name of the metric of this metric. (Yo dawg)
     * 
     * @return Metric's metric name
     */
    String getMetric();

    /**
     * Get the current value of this metric.
     * 
     * @return
     */
    Number getValue();
}
