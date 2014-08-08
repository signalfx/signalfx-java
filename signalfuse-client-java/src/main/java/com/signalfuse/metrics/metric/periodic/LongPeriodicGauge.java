package com.signalfuse.metrics.metric.periodic;

/**
 * A PeriodicGauge that stores long resolution values.
 * @author jack
 */
public interface LongPeriodicGauge extends PeriodicGauge {
    long getLastValue();
}
