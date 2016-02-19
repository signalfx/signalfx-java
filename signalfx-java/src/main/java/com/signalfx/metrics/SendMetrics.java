package com.signalfx.metrics;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

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
        SignalFxReceiverEndpoint endpoint =
                new SignalFxEndpoint(hostUrl.getProtocol(),
                                      hostUrl.getHost(),
                                      hostUrl.getPort());
        AggregateMetricSender mf =
                new AggregateMetricSender("test.SendMetrics",
                                          new HttpProtobufReceiverFactory(
                                                  endpoint)
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
                        SignalFxProtocolBuffers.DataPoint.newBuilder()
                                .setMetric("curtime")
                                .setValue(
                                        SignalFxProtocolBuffers.Datum.newBuilder()
                                                .setIntValue(System.currentTimeMillis()))
                                .addDimensions(
                                        SignalFxProtocolBuffers.Dimension.newBuilder()
                                                .setKey("source")
                                                .setValue("java"))
                                .build());
            } finally {
                i.close();
            }
        }
    }
}
