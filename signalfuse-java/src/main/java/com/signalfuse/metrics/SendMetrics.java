package com.signalfuse.metrics;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import com.signalfuse.metrics.auth.StaticAuthToken;
import com.signalfuse.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfuse.metrics.endpoint.DataPointEndpoint;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.errorhandler.OnSendErrorHandler;
import com.signalfuse.metrics.flush.AggregateMetricSender;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public final class SendMetrics {
    private SendMetrics() {
    }

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));
        final String auth_token = prop.getProperty("auth");
        final String hostUrlStr = prop.getProperty("host");
        final URL hostUrl = new URL(hostUrlStr);
        System.out.println("Auth=" + auth_token + " .. host=" + hostUrl);
        DataPointReceiverEndpoint dataPointEndpoint =
                new DataPointEndpoint(hostUrl.getProtocol(),
                                      hostUrl.getHost(),
                                      hostUrl.getPort());
        AggregateMetricSender mf =
                new AggregateMetricSender("test.SendMetrics",
                                          new HttpDataPointProtobufReceiverFactory(
                                                  dataPointEndpoint)
                                                  .setVersion(2),
                                          new StaticAuthToken(auth_token),
                                          Collections.<OnSendErrorHandler>emptyList());

        int count = 0;
        while (true) {
            System.out.println("Sending data: " + System.currentTimeMillis());
            Thread.sleep(25);
            AggregateMetricSender.Session i = mf.createSession();
            try {
                count += 2;
//                i.incrementCounter("testcounter2", 1);
//                i.setCumulativeCounter("cumulativeCounter", count);
//                i.setGauge("testgauge2", System.currentTimeMillis());
                i.setDatapoint(
                        SignalFuseProtocolBuffers.DataPoint.newBuilder()
                                .setMetric("curtime")
                                .setValue(
                                        SignalFuseProtocolBuffers.Datum.newBuilder()
                                                .setIntValue(System.currentTimeMillis()))
                                .addDimensions(
                                        SignalFuseProtocolBuffers.Dimension.newBuilder()
                                                .setKey("source")
                                                .setValue("java"))
                                .build());
            } finally {
                i.close();
            }
        }
    }
}
