package com.signalfuse.metrics;

import com.signalfuse.metrics.datumhandler.DatumHandlerThreadQueue;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.metric.Counter;
import com.signalfuse.metrics.metric.Gauge;
import com.signalfuse.metrics.metricbuilder.MetricFactory;

/**
 * Date: 4/21/14 Time: 10:16 AM
 * 
 * @author jack
 */
public final class SendMetrics {
    private SendMetrics() {
    }

    public static void main(String[] args) throws InterruptedException {
        final String auth_token;// = "MbQetwyT6bgsHxs3KBY8og";
        final String host;// = "192.168.10.2";
        if (args.length == 2) {
            auth_token = args[0];
            host = args[1];
        } else {
            auth_token = "Zin2Y8Ynx3ol-K0QbgPy3Q";
            host = "192.168.10.2";
        }
        MetricFactory mf = new MetricFactoryBuilder()
                .connectedTo(host, DataPointEndpoint.DEFAULT_PORT)
                .usingToken(auth_token)
                .usingDefaultSource("test")
                .usingDatumHandlerFactory(
                        new DatumHandlerThreadQueue.Factory().setDatumHandlerBufferDelay(1))
                .build();
        final Counter testcounter = mf.createCounter("testcounter2");
        final Gauge testgauge = mf.createGauge("testgauge2");
        while (true) {
            Thread.sleep(100);
            testcounter.incr();
            testgauge.value(System.currentTimeMillis());
        }
    }
}
