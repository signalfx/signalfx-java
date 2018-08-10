package com.signalfx.codahale.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.SfxMetrics;
import com.signalfx.codahale.reporter.IncrementalCounter;
import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfx.metrics.connection.StoredDataPointReceiver;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class SignalFxReporterTest {
    @Test
    public void testReporter() throws InterruptedException {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        MetricRegistry metricRegistery = new MetricRegistry();
        SignalFxReporter reporter = new SignalFxReporter.Builder(metricRegistery, new StaticAuthToken(""), "myserver")
                .setDataPointReceiverFactory(new StaticDataPointReceiverFactory(dbank))
                .setDetailsToAdd(ImmutableSet.of(SignalFxReporter.MetricDetails.COUNT,
                        SignalFxReporter.MetricDetails.MIN, SignalFxReporter.MetricDetails.MAX))
                .build();
        SfxMetrics metrics = new SfxMetrics(metricRegistery, reporter.getMetricMetadata());

        Metric gauge = metricRegistery.register("gauge", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 1;
            }
        });

        final MetricMetadata metricMetadata = reporter.getMetricMetadata();
        metricMetadata.forMetric(metricRegistery.counter("counter"))
                .withMetricName("newname")
                .withSourceName("newsource")
                .withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE);
        metricMetadata.forMetric(metricRegistery.counter("counter2"))
                .withMetricName("newname2");
        metricRegistery.counter("counter").inc();
        metricRegistery.counter("counter").inc();

        metricRegistery.timer("atimer").time().close();

        reporter.report();

        assertEquals(6, dbank.addDataPoints.size());
        assertEquals("newname", dbank.addDataPoints.get(1).getMetric());
        assertEquals("newsource", dbank.addDataPoints.get(1).getDimensions(0).getValue());
        assertEquals("sf_source", dbank.addDataPoints.get(1).getDimensions(0).getKey());
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get(
                "newname"));
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, dbank.registeredMetrics.get(
                "atimer.count"));
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get(
                "atimer.max"));
        assertEquals(2, dbank.lastValueFor("newsource", "newname").getIntValue());

        assertNotNull(dbank.lastValueFor("myserver", "atimer.count"));

        dbank.addDataPoints.clear();
        metricMetadata.forMetric(metricRegistery.counter("raw_counter"))
                .withMetricType(SignalFxProtocolBuffers.MetricType.COUNTER);
        metrics.registerGaugeAsCumulativeCounter("cumulative_counter_callback", new Gauge<Long>() {
            private long i = 0;

            @Override
            public Long getValue() {
                return i++;
            }
        });
        Counter distributedCounter = metricMetadata
                .forMetric(new IncrementalCounter())
                .withMetricName("user_login.hits")
                .withSourceName("webpage")
                .register(metricRegistery);
        assertNotNull(metricRegistery.getCounters().get("webpage.user_login.hits"));
        distributedCounter.inc(123);
        metricRegistery.counter("raw_counter").inc(10);
        metricMetadata.forBuilder(IncrementalCounter.Builder.INSTANCE)
                .withSourceName("asource")
                .withMetricName("name")
                .createOrGet(metricRegistery)
                .inc(1);
        try {
            metricMetadata.forBuilder(SettableLongGauge.Builder.INSTANCE)
                    .withSourceName("asource")
                    .withMetricName("name")
                    .createOrGet(metricRegistery);
            throw new RuntimeException("We shouldn't be able to make it with the same name and different type");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        reporter.report();
        assertEquals(10, dbank.addDataPoints.size());
        assertEquals(10, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(0, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());
        assertEquals(123, dbank.lastValueFor("webpage", "user_login.hits").getIntValue());
        assertEquals(1, dbank.lastValueFor("asource", "name").getIntValue());
        assertEquals(2, dbank.lastValueFor("newsource", "newname").getIntValue());
        distributedCounter.inc(1);
        distributedCounter.inc(3);
        assertEquals(SignalFxProtocolBuffers.MetricType.COUNTER,
                dbank.registeredMetrics.get("user_login.hits"));
        assertEquals(SignalFxProtocolBuffers.MetricType.COUNTER, dbank.registeredMetrics.get("raw_counter"));
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER,
                dbank.registeredMetrics.get("cumulative_counter_callback"));
        metricRegistery.counter("raw_counter").inc(14);
        dbank.addDataPoints.clear();
        metricMetadata.forBuilder(IncrementalCounter.Builder.INSTANCE)
                .withSourceName("asource")
                .withMetricName("name")
                .createOrGet(metricRegistery)
                .inc(3);

        assertEquals(false, metrics.unregister(new Counter()));
        assertEquals(2, metrics.unregister(gauge, metrics.counter("counter")));
        assertEquals(true, dbank.clearValues("newsource", "newname"));
        assertEquals(false, dbank.clearValues("newsource", "newname"));
        reporter.report();
        assertEquals(8, dbank.addDataPoints.size());
        assertEquals(0, dbank.valuesFor("newsource", "newname").size());
        // Users have to use an IncrementalCounter if they want to see the count value of 14 each time
        assertEquals(24, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(1, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());
        assertEquals(4, dbank.lastValueFor("webpage", "user_login.hits").getIntValue());
        assertEquals(3, dbank.lastValueFor("asource", "name").getIntValue());

        try {
            metricMetadata.forBuilder(IncrementalCounter.Builder.INSTANCE).withSourceName("asource")
                    .withMetricName("name")
                    .withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                    .createOrGet(metricRegistery);
            throw new RuntimeException("I expect an error if it's a gauge");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        metricMetadata.forMetric(new Counter())
                .withSourceName("webpage")
                .withMetricName("countstuff")
                .register(metricRegistery);
        try {
            metricMetadata.forBuilder(IncrementalCounter.Builder.INSTANCE)
                    .withSourceName("webpage")
                    .withMetricName("countstuff")
                    .createOrGet(metricRegistery);
            throw new RuntimeException("I expect an already registered metric on this name");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        Timer t = metricMetadata.forBuilder(MetricBuilder.TIMERS)
                .withMetricName("request_time")
                .withDimension("storename", "electronics")
                .createOrGet(metricRegistery);
        Timer.Context c = t.time();
        try {
            System.out.println("Doing store things");
        } finally {
            c.close();
        }
        // Java 7 alternative:
//        try (Timer.Context ignored = t.time()) {
//            System.out.println("Doing store things");
//        }
    }
}
