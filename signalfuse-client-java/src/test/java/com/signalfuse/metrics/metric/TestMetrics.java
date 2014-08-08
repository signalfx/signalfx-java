package com.signalfuse.metrics.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.signalfuse.metrics.MetricFactoryBuilder;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.auth.ConfigAuthToken;
import com.signalfuse.metrics.connection.DataPointReceiver;
import com.signalfuse.metrics.connection.InjectedDataPointReceiverFactory;
import com.signalfuse.metrics.datumhandler.DatumHandlerThreadQueue;
import com.signalfuse.metrics.datumhandler.DirectCallDatumHandler;
import com.signalfuse.metrics.datumhandler.IgnoringDatumHandler;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.signalfuse.metrics.connection.DataPointReceiverFactory;
import com.signalfuse.metrics.connection.StoredDataPointReceiver;
import com.signalfuse.metrics.metricbuilder.MetricFactory;

/**
 * Date: 11/20/13 Time: 2:45 PM
 * 
 * @author jack
 */
public class TestMetrics {

    private static final long UNIT_TEST_SLEEP_TIME = 3;
    private MetricFactoryBuilder builder;

    @Before
    public void init() {
        builder = new MetricFactoryBuilder().usingToken("").usingDatumHandlerFactory(
                new DatumHandlerThreadQueue.Factory()
                        .setDatumHandlerBufferDelay(UNIT_TEST_SLEEP_TIME));
    }

    @Test
    public void testAuthToken() {
        File sessionFile = Paths.get(System.getProperty("user.home"), ".sfsession").toFile();
        if (sessionFile.canRead()) {
            // Only do the test if the config file exists
            new ConfigAuthToken().getAuthToken();
        }
    }

    @Test
    public void testLoggingCounters() {
        MetricFactory metricFactory = builder.usingDataPointReceiverFactory(
                new DataPointReceiverFactory() {
                    public DataPointReceiver createDataPointReceiver(DataPointReceiverEndpoint
                                                                             dataPointEndpoint)
                            throws SignalfuseMetricsException {
                        throw new SignalfuseMetricsException("I never work");
                    }
                }).build();

        Gauge G = metricFactory.createGauge("metricName");
        G.value(10);
        // Our unit test logs shouldn't be spammed
        try {
            Thread.sleep(UNIT_TEST_SLEEP_TIME * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testQueueDatumHandler() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        MetricFactory metricFactory = builder.usingDataPointReceiverFactory(
                new InjectedDataPointReceiverFactory(dbank)).build();
        Gauge G = metricFactory.createGauge("metricName");
        G.value(10);

        for (int i = 0; i < 100 && dbank.addDataPoints.isEmpty(); i++) {
            try {
                Thread.sleep(UNIT_TEST_SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEquals(SignalFuseProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get("metricName"));
        assertEquals(1, dbank.addDataPoints.size());
        assertEquals("metricName", dbank.addDataPoints.get(0).getMetric());
        assertEquals(10, dbank.addDataPoints.get(0).getValue().getIntValue());

        // Address of object should be the same
        assertTrue(metricFactory.createCounter("counterName") == metricFactory
                .createCounter("counterName"));

        metricFactory.createCounter("counterName").incr();
        metricFactory.createCounter("counterName").incr(10);
        for (int i = 0; i < 100 && dbank.addDataPoints.size() <= 1; i++) {
            try {
                Thread.sleep(UNIT_TEST_SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEquals(2, dbank.addDataPoints.size());
        assertEquals("counterName", dbank.addDataPoints.get(1).getMetric());
        assertEquals(11, dbank.addDataPoints.get(1).getValue().getIntValue());
        assertEquals(SignalFuseProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get("metricName"));
        assertEquals(SignalFuseProtocolBuffers.MetricType.COUNTER, dbank.registeredMetrics.get("counterName"));
    }

    @Test
    public void testDirectCallDatumHandler() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        MetricFactory metricFactory = builder
                .usingDataPointReceiverFactory(new InjectedDataPointReceiverFactory(dbank))
                .usingDatumHandlerFactory(new DirectCallDatumHandler.Factory()).build();
        Gauge G = metricFactory.createGauge("metricName");
        G.value(10);
        assertEquals(1, dbank.addDataPoints.size());
        assertEquals("metricName", dbank.addDataPoints.get(0).getMetric());
        assertEquals(10, dbank.addDataPoints.get(0).getValue().getIntValue());
    }

    @Test
    public void testIgnoringDatumhandler() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        MetricFactory metricFactory = builder
                .usingDatumHandlerFactory(new IgnoringDatumHandler.Factory())
                .usingDataPointReceiverFactory(new InjectedDataPointReceiverFactory(dbank)).build();
        Gauge G = metricFactory.createGauge("metricName");
        G.value(10);

        assertEquals(0, dbank.addDataPoints.size());
    }

    @Test
    public void testLatencyGauge() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        MetricFactory metricFactory = builder.usingDataPointReceiverFactory(
                new InjectedDataPointReceiverFactory(dbank)).build();
        Sample S = metricFactory.createSample("latency");
        Sample.Timer timer = null;
        try {
            timer = S.time();
            try {
                Thread.sleep(UNIT_TEST_SLEEP_TIME * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Ignore it
            }
        } finally {
            if (timer != null) {
                timer.close();
            }
        }
        for (int i = 0; i < 100 && dbank.addDataPoints.isEmpty(); i++) {
            try {
                Thread.sleep(UNIT_TEST_SLEEP_TIME);
            } catch (InterruptedException e) {
                // Let queue flush
                e.printStackTrace();
            }
        }
        assert (dbank.addDataPoints.get(0).getValue().getIntValue() > UNIT_TEST_SLEEP_TIME * 1.5);
        assertEquals(SignalFuseProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get("latency"));
    }

    @Test
    public void testPeriodGauge() {
        StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        MetricFactory metricFactory = builder
                .usingDataPointReceiverFactory(new InjectedDataPointReceiverFactory(dbank))
                .usingDatumHandlerFactory(new DirectCallDatumHandler.Factory()).build();
        Gauge g = metricFactory.createGauge("timed");
        final AtomicLong l = new AtomicLong(0);
        metricFactory.createPeriodicGauge(g, TimeUnit.MILLISECONDS, UNIT_TEST_SLEEP_TIME * 2,
                new LongCallback() {
                    public long getValue() {
                        return l.incrementAndGet();
                    }
                });

        for (int i = 0; i < 100 && dbank.addDataPoints.size() < 3; i++) {
            try {
                Thread.sleep(UNIT_TEST_SLEEP_TIME);
            } catch (InterruptedException e) {
                // Let queue flush
                e.printStackTrace();
            }
        }
        assert (dbank.addDataPoints.get(0).getValue().getIntValue() == 1);
        assert (dbank.addDataPoints.get(1).getValue().getIntValue() == 2);
        assert (dbank.addDataPoints.get(2).getValue().getIntValue() == 3);
    }
}
