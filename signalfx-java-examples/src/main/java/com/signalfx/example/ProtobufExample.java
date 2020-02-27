package com.signalfx.example;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.connection.HttpEventProtobufReceiverFactory;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class ProtobufExample {

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));
        String token = prop.getProperty("token");
        String hostUrlStr = prop.getProperty("host");
        URL hostUrl = new URL(hostUrlStr);
        System.out.println("token=" + token + " host=" + hostUrl);
        SignalFxEndpoint endpoint = new SignalFxEndpoint(hostUrl.getProtocol(), hostUrl.getHost(), hostUrl.getPort());
        String sourceName = "my.source";
        AggregateMetricSender sender = new AggregateMetricSender(
                sourceName,
                new HttpDataPointProtobufReceiverFactory(endpoint).setVersion(2),
                new HttpEventProtobufReceiverFactory(endpoint),
                new StaticAuthToken(token),
                Collections.singleton(error -> System.out.println("Unable to POST metrics: " + error.getMessage()))
        );

        int j = 0;
        while (true) {
            // session should be recreated after every session.close().
            AggregateMetricSender.Session session = sender.createSession();

            System.out.println("Setting datapoint " + (j));
            SignalFxProtocolBuffers.DataPoint pt = SignalFxProtocolBuffers.DataPoint.newBuilder()
                    .setMetric("protobuf.test.cpu")
                    .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                    .setValue(SignalFxProtocolBuffers.Datum.newBuilder().setIntValue(j % 3))
                    .addDimensions(buildDimension("host", "myhost"))
                    .addDimensions(buildDimension("service", "myservice"))
                    .build();
            session.setDatapoint(pt);

            if (j % 10 == 0) {
                System.out.println("Setting Event  " + j / 10);
                SignalFxProtocolBuffers.Event event = SignalFxProtocolBuffers.Event.newBuilder()
                        .setEventType("Deployments")
                        .setCategory(SignalFxProtocolBuffers.EventCategory.USER_DEFINED)
                        .setTimestamp(System.currentTimeMillis())
                        .addDimensions(buildDimension("host", "myhost"))
                        .addDimensions(buildDimension("service", "myservice"))
                        .addProperties(buildProperty("version", j / 3))
                        .build();
                session.setEvent(event);
            }

            System.out.println("Flushing datapoints and events");
            session.close();// this flushes any remaining datapoints and events

            j++;
            Thread.sleep(500);
        }
    }

    private static SignalFxProtocolBuffers.Dimension buildDimension(String key, String value) {
        return SignalFxProtocolBuffers.Dimension.newBuilder().setKey(key).setValue(value).build();
    }

    // property value can be int, double, bool, string
    private static SignalFxProtocolBuffers.Property buildProperty(String key, int value) {
        SignalFxProtocolBuffers.PropertyValue propVal = SignalFxProtocolBuffers.PropertyValue.newBuilder().setIntValue(value).build();
        return SignalFxProtocolBuffers.Property.newBuilder().setKey(key).setValue(propVal).build();
    }

}
