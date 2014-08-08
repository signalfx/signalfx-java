package com.signalfuse.metrics.metricbuilder.internal;

import java.util.concurrent.TimeUnit;

import com.signalfuse.metrics.datumhandler.DatumHandler;
import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.CumulativeCounter;
import com.signalfuse.metrics.metric.Sample;
import com.signalfuse.metrics.metric.internal.CounterImpl;
import com.signalfuse.metrics.metric.internal.GaugeImpl;
import com.signalfuse.metrics.metric.internal.CumulativeCounterImpl;
import com.signalfuse.metrics.metric.internal.SampleImpl;
import com.signalfuse.metrics.metric.periodic.DoubleCallback;
import com.signalfuse.metrics.metric.periodic.DoublePeriodicGauge;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.metric.periodic.LongPeriodicGauge;
import com.signalfuse.metrics.metric.periodic.internal.PeriodicGaugeScheduler;
import com.signalfuse.metrics.metricbuilder.MetricFactory;

public class MetricFactoryImpl implements MetricFactory {
    private final String defaultSourceName;
    private final PeriodicGaugeScheduler periodicGaugeScheduler;
    private final CounterImpl.Factory counterFactory;
    private final CumulativeCounterImpl.Factory cumulativeCounterFactory;
    private final GaugeImpl.Factory gaugeFactory;
    private final SampleImpl.Factory sampleFactory;

    public MetricFactoryImpl(String defaultSourceName, DatumHandler datumHandler,
                             PeriodicGaugeScheduler periodicGaugeScheduler) {
        this.defaultSourceName = defaultSourceName;
        this.periodicGaugeScheduler = periodicGaugeScheduler;
        this.counterFactory = new CounterImpl.Factory(datumHandler);
        this.cumulativeCounterFactory = new CumulativeCounterImpl.Factory(datumHandler);
        this.gaugeFactory = new GaugeImpl.Factory(datumHandler);
        this.sampleFactory = new SampleImpl.Factory(datumHandler);
    }

    @Override public String getDefaultSource() {
        return defaultSourceName;
    }

    @Override public Gauge createGauge(String metricName) {
        return createGauge(defaultSourceName, metricName);
    }

    @Override public Gauge createGauge(String sourceName, String metricName) {
        return gaugeFactory.getMetric(sourceName, metricName);
    }

    @Override public Counter createCounter(String metricName) {
        return createCounter(defaultSourceName, metricName);
    }

    @Override public Counter createCounter(String sourceName, String metricName) {
        return counterFactory.getMetric(sourceName, metricName);
    }

    @Override public CumulativeCounter createCumulativeCounter(String metricName) {
        return createCumulativeCounter(defaultSourceName, metricName);
    }

    @Override public CumulativeCounter createCumulativeCounter(String sourceName, String metricName) {
        return cumulativeCounterFactory.getMetric(sourceName, metricName);
    }

    @Override public Sample createSample(String sourceName, String metricName) {
        return sampleFactory.getMetric(sourceName, metricName);
    }

    @Override public Sample createSample(String metricName) {
        return createSample(defaultSourceName, metricName);
    }

    @Override public LongPeriodicGauge createPeriodicGauge(Gauge gauge, TimeUnit timeUnit, long unitPeriod,
                                                 LongCallback callback) {
        return periodicGaugeScheduler.addPeriodicGauge(gauge, timeUnit, unitPeriod, callback);
    }

    @Override public DoublePeriodicGauge createPeriodicGauge(Gauge gauge, TimeUnit timeUnit, long unitPeriod,
                                                   DoubleCallback callback) {
        return periodicGaugeScheduler.addPeriodicGauge(gauge, timeUnit, unitPeriod, callback);
    }
}
