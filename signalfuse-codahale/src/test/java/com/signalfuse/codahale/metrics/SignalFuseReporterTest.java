package com.signalfuse.codahale.metrics;

import org.junit.Test;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.signalfuse.codahale.reporter.SfUtil;
import com.signalfuse.codahale.reporter.SignalFuseReporter;
import com.signalfuse.metrics.auth.StaticAuthToken;
import com.signalfuse.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfuse.metrics.connection.StoredDataPointReceiver;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SignalFuseReporterTest {
    @Test
    public void testReporter() throws InterruptedException {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        MetricRegistry metricRegistery = new MetricRegistry();
        SignalFuseReporter reporter = new SignalFuseReporter.Builder(metricRegistery, new StaticAuthToken(""), "myserver")
                .setDataPointReceiverFactory(new StaticDataPointReceiverFactory(dbank))
                .setDetailsToAdd(ImmutableSet.of(SignalFuseReporter.MetricDetails.COUNT,
                        SignalFuseReporter.MetricDetails.MIN, SignalFuseReporter.MetricDetails.MAX))
                .build();

        metricRegistery.register("gauge", new Gauge<Integer>() {
            public Integer getValue() {
                return 1;
            }
        });

        reporter.getMetricMetadata().tagMetric(metricRegistery.counter("counter"))
                .withMetricName("newname")
                .withSourceName("newsource")
                .withMetricType(SignalFuseProtocolBuffers.MetricType.GAUGE);
        reporter.getMetricMetadata().tagMetric(metricRegistery.counter("counter2"))
                .withMetricName("newname2");
        metricRegistery.counter("counter").inc();
        metricRegistery.counter("counter").inc();

        metricRegistery.timer("atimer").time().close();

        reporter.report();

        assertEquals(6, dbank.addDataPoints.size());
        assertEquals("newname", dbank.addDataPoints.get(1).getMetric());
        assertEquals("newsource", dbank.addDataPoints.get(1).getSource());
        assertEquals(SignalFuseProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get(
                "newname"));
        assertEquals(SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER, dbank.registeredMetrics.get("atimer.count"));
        assertEquals(SignalFuseProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get("atimer.max"));
        assertEquals(2, dbank.lastValueFor("newsource", "newname").getIntValue());

        assertNotNull(dbank.lastValueFor("myserver", "atimer.count"));

        dbank.addDataPoints.clear();
        reporter.getMetricMetadata().tagMetric(metricRegistery.counter("raw_counter"))
                .withMetricType(SignalFuseProtocolBuffers.MetricType.COUNTER);
        SfUtil.cumulativeCounter(metricRegistery, "cumulative_counter_callback", reporter.getMetricMetadata(), new Gauge<Long>(){
            private long i = 0;
            @Override public Long getValue() {
                return i++;
            }
        });
        metricRegistery.counter("raw_counter").inc(10);
        reporter.report();
        assertEquals(8, dbank.addDataPoints.size());
        assertEquals(10, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(0, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());
        assertEquals(SignalFuseProtocolBuffers.MetricType.COUNTER, dbank.registeredMetrics.get("raw_counter"));
        assertEquals(SignalFuseProtocolBuffers.MetricType.CUMULATIVE_COUNTER, dbank.registeredMetrics.get("cumulative_counter_callback"));
        metricRegistery.counter("raw_counter").inc(14);
        dbank.addDataPoints.clear();
        reporter.report();
        assertEquals(8, dbank.addDataPoints.size());
        assertEquals(14, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(1, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());
    }
}
