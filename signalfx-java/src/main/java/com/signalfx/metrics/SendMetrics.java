package com.signalfx.metrics;

import com.google.common.base.Preconditions;
import com.signalfx.metrics.errorhandler.MetricError;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * An example application to test sending metrics with this library.
 *
 * @author jack
 * @author max
 */
public final class SendMetrics {

    private SendMetrics() {}

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));

        String token = Preconditions.checkNotNull(prop.getProperty("auth"), "No auth token set");
        String host = Preconditions.checkNotNull(prop.getProperty("host"), "No endpoint set");
        URL hostUrl = new URL(host);

        System.out.printf("Sending metrics to %s (X-SF-Token: %s) ...%n", hostUrl, token);

        SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint(
                hostUrl.getProtocol(),
                hostUrl.getHost(),
                hostUrl.getPort());

        OnSendErrorHandler errorHandler = new OnSendErrorHandler() {
            @Override
            public void handleError(MetricError metricError) {
                System.err.printf("Error %s sending data: %s%n",
                        metricError.getMetricErrorType(),
                        metricError.getMessage());
                metricError.getException().printStackTrace(System.err);
            }
        };

        AggregateMetricSender mf = new AggregateMetricSender(
                "test.SendMetrics",
                new HttpDataPointProtobufReceiverFactory(endpoint).setVersion(2),
                new StaticAuthToken(token),
                Collections.singletonList(errorHandler));

        while (true) {
            Thread.sleep(250);
            AggregateMetricSender.Session i = mf.createSession();
            try {
                i.setDatapoint(SignalFxProtocolBuffers.DataPoint.newBuilder()
                        .setMetric("curtime")
                        .setValue(SignalFxProtocolBuffers.Datum.newBuilder()
                                .setIntValue(System.currentTimeMillis()))
                        .addDimensions(
                                SignalFxProtocolBuffers.Dimension.newBuilder()
                                        .setKey("source")
                                        .setValue("java")).build());
            } finally {
                System.out.printf("Sending data at %s%n", new Date());
                i.close();
            }
        }
    }
}
