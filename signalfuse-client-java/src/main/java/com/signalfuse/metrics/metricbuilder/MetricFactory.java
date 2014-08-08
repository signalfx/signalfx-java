package com.signalfuse.metrics.metricbuilder;

import java.util.concurrent.TimeUnit;

import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.CumulativeCounter;
import com.signalfuse.metrics.metric.Sample;
import com.signalfuse.metrics.metric.periodic.DoubleCallback;
import com.signalfuse.metrics.metric.periodic.DoublePeriodicGauge;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.metric.periodic.LongPeriodicGauge;

/**
 * Factory to create metric instances.
 * 
 * @author jack
 */
public interface MetricFactory {

    String getDefaultSource();

    Gauge createGauge(String sourceName, String metricName);

    Gauge createGauge(String metricName);

    Counter createCounter(String sourceName, String metricName);

    Counter createCounter(String metricName);

    CumulativeCounter createCumulativeCounter(String sourceName, String metricName);

    CumulativeCounter createCumulativeCounter(String metricName);

    Sample createSample(String sourceName, String metricName);

    Sample createSample(String metricName);

    LongPeriodicGauge createPeriodicGauge(final Gauge gauge, TimeUnit timeUnit,
                                          long unitPeriod, final LongCallback callback);

    DoublePeriodicGauge createPeriodicGauge(final Gauge gauge, TimeUnit timeUnit,
                                            long unitPeriod, final DoubleCallback callback);
}
