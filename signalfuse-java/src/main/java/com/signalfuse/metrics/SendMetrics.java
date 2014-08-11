package com.signalfuse.metrics;

import java.util.Collections;
import com.signalfuse.metrics.auth.StaticAuthToken;
import com.signalfuse.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.flush.AggregateMetricSender;

public final class SendMetrics {
    private SendMetrics() {
    }

    public static void main(String[] args) throws Exception {
        final String auth_token;// = "MbQetwyT6bgsHxs3KBY8og";
        final String host;// = "192.168.10.2";
        if (args.length == 2) {
            auth_token = args[0];
            host = args[1];
        } else {
            auth_token = "OO9aPaftRvx_bJMc7aD8OQ";
            host = "lb-lab1--bbaa.int.signalfuse.com";
        }
        DataPointReceiverEndpoint dataPointEndpoint = new DataPointEndpoint(host,
                DataPointEndpoint.DEFAULT_PORT);
        AggregateMetricSender mf = new AggregateMetricSender("test",
                new HttpDataPointProtobufReceiverFactory()
                        .createDataPointReceiver(dataPointEndpoint),
                new StaticAuthToken(auth_token),
                Collections.<OnSendErrorHandler>emptyList());
        int count = 0;
        while (true) {
            Thread.sleep(10);
            AggregateMetricSender.Session i = mf.createSession();
            try {
                count += 2;
                i.incrementCounter("testcounter2", 1);
                i.setCumulativeCounter("cumulativeCounter", count);
                i.setGauge("testgauge2", System.currentTimeMillis());
            } finally {
                i.close();
            }
        }
    }
}
