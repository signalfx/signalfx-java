package com.signalfx.example;

import java.io.FileInputStream;
import java.lang.String;
import java.net.URL;
import java.util.*;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.connection.HttpEventProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.MetricError;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/*
    An example class for sending datapoints and events using Protobuf.
 */
public class ProtobufExample {

    public static void main(String[] args) throws Exception {

        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));
        final String auth_token = prop.getProperty("auth");
        final String hostUrlStr = prop.getProperty("host");
        final URL hostUrl = new URL(hostUrlStr);
        System.out.println("Auth=" + auth_token + " .. host=" + hostUrl);
        SignalFxReceiverEndpoint signalFxEndpoint = new SignalFxEndpoint(hostUrl.getProtocol(),
                hostUrl.getHost(), hostUrl.getPort());

        AggregateMetricSender mf = new AggregateMetricSender("test.SendMetrics",
                new HttpDataPointProtobufReceiverFactory(signalFxEndpoint).setVersion(2),
                new HttpEventProtobufReceiverFactory(signalFxEndpoint),
                new StaticAuthToken(auth_token),
                Collections.<OnSendErrorHandler> singleton(new OnSendErrorHandler() {
                    @Override
                    public void handleError(MetricError metricError) {
                        System.out.println("Unable to POST metrics: " + metricError.getMessage());
                    }
                }));

        int j = 0;
        while(true) {
            // session should be recreated after every sessionObj.close().
            AggregateMetricSender.Session i = mf.createSession();

            System.out.println("Setting datapoint " + (j));
            i.setDatapoint(
                SignalFxProtocolBuffers.DataPoint.newBuilder()
                        .setMetric("test.cpu")
                        .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                        .setValue(
                                SignalFxProtocolBuffers.Datum.newBuilder()
                                        .setIntValue(j%3))
                        .addDimensions(getDimensionAsProtobuf("host", "myhost"))
                        .addDimensions(getDimensionAsProtobuf("service", "myservice"))
                        .build());
            
            if(j%3 == 0){
                System.out.println("Setting Event  " + j/3);
                i.setEvent(
                    SignalFxProtocolBuffers.Event.newBuilder()
                        .setEventType("Deployments")
                        .setCategory(SignalFxProtocolBuffers.EventCategory.USER_DEFINED)
                        .setTimestamp(System.currentTimeMillis())
                        .addDimensions(getDimensionAsProtobuf("host", "myhost"))
                        .addDimensions(getDimensionAsProtobuf("service", "myservice"))
                        .addProperties(getPropertyAsProtobuf("version", j/3))
                        .build());
            } 

            System.out.println("Flushing set datapoints and events");
            i.close(); // close session resource to flush the set datapoints and events
            j++;
            Thread.sleep(500);
        }
    }

    private static SignalFxProtocolBuffers.Dimension getDimensionAsProtobuf(String key, String value){
        return SignalFxProtocolBuffers.Dimension.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
    }

    // property value can be int, double, bool, string
    private static SignalFxProtocolBuffers.Property getPropertyAsProtobuf(String key, int value){
        return SignalFxProtocolBuffers.Property.newBuilder()
                .setKey(key)
                .setValue(
                        SignalFxProtocolBuffers.PropertyValue.newBuilder()
                                .setIntValue(value)
                                .build())
                .build();
    }

}