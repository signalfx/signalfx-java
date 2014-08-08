package com.signalfuse.metrics.jmx;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metric.Metric;
import com.signalfuse.metrics.metric.CumulativeCounter;
import com.signalfuse.metrics.metric.Sample;
import com.signalfuse.metrics.metric.periodic.DoubleCallback;
import com.signalfuse.metrics.metric.periodic.DoublePeriodicGauge;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.metric.periodic.LongPeriodicGauge;
import com.signalfuse.metrics.metricbuilder.MetricFactory;
import com.signalfuse.metrics.metricbuilder.MetricFactoryWrapper;

public class JmxAwareMetricFactory implements MetricFactory {
    private static final Logger log = LoggerFactory.getLogger(JmxAwareMetricFactory.class);

    private final MBeanServer mbeanServer;
    private final MetricFactory delegate;
    private final Set<ObjectName> createdMbeanObjects = Collections
            .synchronizedSet(new HashSet<ObjectName>());

    public static MetricFactoryWrapper wrapper() {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        return new MetricFactoryWrapper() {
            @Override public MetricFactory wrap(MetricFactory wrapped) {
                return new JmxAwareMetricFactory(mbeanServer, wrapped);
            }
        };
    }

    private JmxAwareMetricFactory(MBeanServer mbeanServer, MetricFactory delegate) {
        this.mbeanServer = mbeanServer;
        this.delegate = delegate;
    }

    @Override public String getDefaultSource() {
        return delegate.getDefaultSource();
    }

    @Override public Gauge createGauge(String sourceName, String metricName) {
        return wrap(delegate.createGauge(sourceName, metricName));
    }

    @Override public Gauge createGauge(String metricName) {
        return wrap(delegate.createGauge(metricName));
    }

    @Override public Counter createCounter(String sourceName, String metricName) {
        return wrap(delegate.createCounter(sourceName, metricName));
    }

    @Override public Counter createCounter(String metricName) {
        return wrap(delegate.createCounter(metricName));
    }

    @Override public CumulativeCounter createCumulativeCounter(String sourceName, String metricName) {
        return wrap(delegate.createCumulativeCounter(sourceName, metricName));
    }

    @Override public CumulativeCounter createCumulativeCounter(String metricName) {
        return wrap(delegate.createCumulativeCounter(metricName));
    }

    @Override public Sample createSample(String sourceName, String metricName) {
        return wrap(delegate.createSample(sourceName, metricName));
    }

    @Override public Sample createSample(String metricName) {
        return wrap(delegate.createSample(metricName));
    }

    @Override public LongPeriodicGauge createPeriodicGauge(Gauge gauge, TimeUnit timeUnit, long unitPeriod,
                                                 LongCallback callback) {
        return delegate.createPeriodicGauge(gauge, timeUnit, unitPeriod, callback);
    }

    @Override public DoublePeriodicGauge createPeriodicGauge(Gauge gauge, TimeUnit timeUnit, long unitPeriod,
                                                   DoubleCallback callback) {
        return delegate.createPeriodicGauge(gauge, timeUnit, unitPeriod, callback);
    }

    private <T extends Metric> T wrap(T wrapped) {
        try {
            ObjectName objectName = objectName(wrapped);
            if (createdMbeanObjects.add(objectName)) {
                JmxMetricMBean mbean = new JmxMetricMBean(wrapped);
                mbeanServer.registerMBean(mbean, objectName);
            }
        } catch (JMException e) {
            log.warn("Error registering MBean for metric=" + wrapped.getMetric() + " and source="
                    + wrapped.getSource(), e);
        }
        return wrapped;
    }

    private static ObjectName objectName(Metric metric) throws MalformedObjectNameException {
        Hashtable<String, String> props = new Hashtable<String, String>(2);
        props.put("metric", metric.getMetric());
        props.put("source", metric.getSource());
        return new ObjectName("sf.metrics", props);
    }

    @MXBean
    public interface MetricMBean {
        double getDoubleValue();

        long getLongValue();
    }

    private static class JmxMetricMBean implements MetricMBean {
        private final Metric metric;

        private JmxMetricMBean(Metric metric) throws MalformedObjectNameException {
            this.metric = metric;
        }

        @Override public double getDoubleValue() {
            return metric.getValue().doubleValue();
        }

        @Override public long getLongValue() {
            return metric.getValue().longValue();
        }
    }
}
