package com.signalfx.codahale.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.SfxMetrics;
import com.signalfx.codahale.reporter.IncrementalCounter;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfx.metrics.connection.StoredDataPointReceiver;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class SignalFxReporterTest {

    private static final String SOURCE_NAME = "myserver";

    private MetricRegistry metricRegistry;
    private SignalFxReporter reporter;
    private SfxMetrics sfxMetrics;
    private StoredDataPointReceiver dbank;

    @Before
    public void setup() {
        metricRegistry = new MetricRegistry();
        dbank = new StoredDataPointReceiver();
        reporter = new SignalFxReporter
                .Builder(metricRegistry, new StaticAuthToken(""), SOURCE_NAME)
                .setDataPointReceiverFactory(new StaticDataPointReceiverFactory(dbank))
                .setDetailsToAdd(ImmutableSet.of(SignalFxReporter.MetricDetails.COUNT,
                        SignalFxReporter.MetricDetails.MIN,
                        SignalFxReporter.MetricDetails.MAX))
                .build();
        sfxMetrics = new SfxMetrics(metricRegistry, reporter.getMetricMetadata());
    }

    @Test
    public void testEmptyDataBank() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());
    }

    @Test
    public void testGauge() {
        Gauge<Integer> gauge = sfxMetrics.registerGauge("gauge", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return 1;
            }
        });
        reporter.report();
        assertEquals(1, dbank.addDataPoints.size());
        assertTrue(sfxMetrics.unregister(gauge));
    }

    @Test
    public void testCounter() {
        Counter counter = sfxMetrics.counter("counter");
        counter.inc();
        counter.inc();
        reporter.report();
        assertEquals(1, dbank.addDataPoints.size());
        // counter without dimensions doesn't get into MetricMetadata, so we cannot unregister
        // from SfxMetrics. But we can remove it from MetricRegistry.
        assertFalse(sfxMetrics.unregister(counter));
        assertTrue(metricRegistry.remove("counter"));
    }

    @Test
    public void testCounterWithDimensions() {
        Counter counter = sfxMetrics.counter("counter", "dimName", "dimValue");
        counter.inc();
        counter.inc();
        reporter.report();
        assertEquals(1, dbank.addDataPoints.size());
        assertTrue(sfxMetrics.unregister(counter));
    }

    @Test
    public void testRawCounter() {
        Counter rawCounter = reporter.getMetricMetadata()
                .forMetric(metricRegistry.counter("rawCounter"))
                .withMetricType(SignalFxProtocolBuffers.MetricType.COUNTER).metric();
        rawCounter.inc(10);
        reporter.report();
        rawCounter.inc(14);
        reporter.report();
        // Users have to use an IncrementalCounter if they want to see the count value of 14 each time
        assertEquals(24, dbank.lastValueFor(SOURCE_NAME, "rawCounter").getIntValue());
        assertEquals(SignalFxProtocolBuffers.MetricType.COUNTER,
                dbank.registeredMetrics.get("rawCounter"));
    }

    @Test
    public void testIncrementalCounter() {
        IncrementalCounter incrementalCounter = sfxMetrics
                .incrementalCounter("incrementalCounter");
        incrementalCounter.inc(3);
        assertEquals(false, sfxMetrics.unregister(new Counter()));
        reporter.report();
        assertEquals(1, dbank.addDataPoints.size());
        assertEquals(3, dbank.lastValueFor(SOURCE_NAME, "incrementalCounter").getIntValue());
    }

    @Test
    public void testTimer() throws Exception {
        Timer timer = sfxMetrics.timer("timer", "dimName", "dimValue");
        // track time taken.
        for (int i = 0; i < 4; i++) {
            Timer.Context context = timer.time();
            try {
                Thread.sleep(10 + i * 10);
            } finally {
                context.close();
            }
        }
        /*
        Java 7 alternative:
        try (Timer.Context ignored = t.time()) {
            System.out.println("Doing store things");
        }
        */
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER,
                dbank.registeredMetrics.get("timer.count"));
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE,
                dbank.registeredMetrics.get("timer.max"));
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE,
                dbank.registeredMetrics.get("timer.min"));
        assertTrue(sfxMetrics.unregister(timer));
    }

    @Test
    public void testResettingTimer() {
        Timer resettingTimer = sfxMetrics.resettingTimer("resettingTimer", "dimName", "dimValue");
        resettingTimer.update(20, TimeUnit.MILLISECONDS);
        resettingTimer.update(30, TimeUnit.MILLISECONDS);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(2, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.count").getIntValue());
        assertEquals(20000000, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.min").getIntValue());
        assertEquals(30000000, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.max").getIntValue());
        dbank.addDataPoints.clear();
        resettingTimer.update(25, TimeUnit.MILLISECONDS);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(3, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.count").getIntValue());
        assertEquals(25000000, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.min").getIntValue());
        assertEquals(25000000, dbank.lastValueFor(SOURCE_NAME, "resettingTimer.max").getIntValue());
    }

    @Test
    public void testHistogram() {
        Histogram histogram = sfxMetrics.histogram("histogram", "dimName", "dimValue");
        histogram.update(20);
        histogram.update(30);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(2, dbank.lastValueFor(SOURCE_NAME, "histogram.count").getIntValue());
        assertEquals(20, dbank.lastValueFor(SOURCE_NAME, "histogram.min").getIntValue());
        assertEquals(30, dbank.lastValueFor(SOURCE_NAME, "histogram.max").getIntValue());
        dbank.addDataPoints.clear();
        histogram.update(25);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(3, dbank.lastValueFor(SOURCE_NAME, "histogram.count").getIntValue());
        assertEquals(20, dbank.lastValueFor(SOURCE_NAME, "histogram.min").getIntValue());
        assertEquals(30, dbank.lastValueFor(SOURCE_NAME, "histogram.max").getIntValue());
    }

    @Test
    public void testResettingHistogram() {
        Histogram resettingHistogram = sfxMetrics.resettingHistogram("resettingHistogram");
        resettingHistogram.update(20);
        resettingHistogram.update(30);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(2, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.count").getIntValue());
        assertEquals(20, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.min").getIntValue());
        assertEquals(30, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.max").getIntValue());
        dbank.addDataPoints.clear();
        resettingHistogram.update(25);
        reporter.report();
        assertEquals(3, dbank.addDataPoints.size());
        assertEquals(3, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.count").getIntValue());
        assertEquals(25, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.min").getIntValue());
        assertEquals(25, dbank.lastValueFor(SOURCE_NAME, "resettingHistogram.max").getIntValue());
    }

    @Test
    public void testReportingGaugeAsCummulativeCounter() {
        String cumulativeCounterCallback = "cumulativeCounterCallback";
        sfxMetrics.registerGaugeAsCumulativeCounter(cumulativeCounterCallback,
                new Gauge<Long>() {
                    private long i = 0;

                    @Override
                    public Long getValue() {
                        return i++;
                    }
                });
        reporter.report();
        assertEquals(0, dbank.lastValueFor(SOURCE_NAME, cumulativeCounterCallback).getIntValue());
        reporter.report();
        reporter.report();
        assertEquals(2, dbank.lastValueFor(SOURCE_NAME, cumulativeCounterCallback).getIntValue());
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER,
                dbank.registeredMetrics.get(cumulativeCounterCallback));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateMetricRegistration() {
        sfxMetrics.counter("countstuff");
        sfxMetrics.incrementalCounter("countstuff");
        fail("I expect an already registered metric on this name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdatingMetricTypeToIncompatibleValue() {
        reporter.getMetricMetadata()
                .forBuilder(IncrementalCounter.Builder.INSTANCE)
                .withSourceName(SOURCE_NAME)
                .withMetricName("name")
                .createOrGet(metricRegistry)
                .inc(3);
        reporter.getMetricMetadata()
                .forBuilder(IncrementalCounter.Builder.INSTANCE)
                .withSourceName(SOURCE_NAME)
                .withMetricName("name")
                .withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                .createOrGet(metricRegistry);
        fail("I expect an error if it's a gauge");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrGetMetricSameNameDifferentType() {
        reporter.getMetricMetadata().forBuilder(IncrementalCounter.Builder.INSTANCE)
                .withSourceName("asource")
                .withMetricName("name")
                .createOrGet(metricRegistry)
                .inc(1);
        reporter.getMetricMetadata().forBuilder(SettableLongGauge.Builder.INSTANCE)
                .withSourceName("asource")
                .withMetricName("name")
                .createOrGet(metricRegistry);
        fail("We shouldn't be able to make it with the same name and different type");
    }
}
