package com.splunk.signalfx;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MetricSenderTest {

    private static final int NUM_PTS_PER_THREAD = 10_000;

    @Test
    public void multiplePoints() throws InterruptedException, IOException {
        testSend(0);
        testSend(100);
    }

    private static void testSend(int millis) throws InterruptedException, IOException {
        FakeClient client = new FakeClient();
        MetricSender s = buildMetricSender(client);
        s.start();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                s.recordGaugeValue("aaa.bbb", 42, ImmutableMap.of("foo", "bar"), 1000);
                s.recordGaugeValue("ccc.ddd", 3.14, ImmutableMap.of("baz", "glarch"), 1000);
            }
            Thread.sleep(millis);
        }
        s.stop();
        assertEquals(200, client.getNumPts());
    }

    @Test
    public void singlePoint() throws InterruptedException, IOException {
        for (int i = 0; i < 100; i++) {
            sendSinglePoint();
        }
    }

    private static void sendSinglePoint() throws InterruptedException, IOException {
        FakeClient client = new FakeClient();
        MetricSender s = buildMetricSender(client);
        s.start();
        s.recordGaugeValue("aaa.bbb", 42, ImmutableMap.of("foo", "bar"), 1000);
        s.stop();
        assertEquals(1, client.getNumPts());
    }

    @Test
    public void parallelSend() throws IOException, InterruptedException {
        FakeClient client = new FakeClient();
        MetricSender s = buildMetricSender(client);
        s.start();
        int numThreads = 100;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = mkThread(s);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        s.stop();
        assertEquals(numThreads * NUM_PTS_PER_THREAD, client.getNumPts());
    }

    private static Thread mkThread(MetricSender s) {
        return new Thread(() -> {
            ImmutableMap<String, String> dims = ImmutableMap.of("a", "b");
            for (int i = 0; i < NUM_PTS_PER_THREAD; i++) {
                s.recordGaugeValue("foo.bar", 42, dims);
            }
        });
    }

    private static MetricSender buildMetricSender(FakeClient client) {
        return MetricSender.builder()
                    .withDefaultParams()
                    .ingestUrl("http://www.example.com/ingest")
                    .token("s3cr3t")
                    .encoder(new JsonEncoder())
                    .client(client)
                    .build();
    }
}
