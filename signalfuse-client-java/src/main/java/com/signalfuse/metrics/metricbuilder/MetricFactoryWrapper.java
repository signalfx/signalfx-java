package com.signalfuse.metrics.metricbuilder;

/**
 * This interface can be used to wrap a MetricFactory, useful for intercepting metric creation and
 * wrapping it with additional logic.
 */
public interface MetricFactoryWrapper {
    MetricFactory wrap(MetricFactory wrapped);
}
