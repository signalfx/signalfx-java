package com.signalfuse.metrics.metric.periodic;

/**
 * A PeriodicGauge that stores double resolution values.
 * @author jack
 */
public interface DoublePeriodicGauge extends PeriodicGauge {
    double getLastValue();
}
