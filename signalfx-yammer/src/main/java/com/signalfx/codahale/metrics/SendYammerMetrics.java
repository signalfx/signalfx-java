package com.signalfx.codahale.metrics;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

import com.google.common.collect.ImmutableSet;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;

import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.SfUtil;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfx.metrics.connection.StoredDataPointReceiver;

import com.signalfx.metrics.errorhandler.MetricError;

public final class SendYammerMetrics {
    private SendYammerMetrics() {
    }

    public static void main(String[] args) throws Exception {
    	
    	System.out.println("Running [com.signalfx.metrics.SendYammerMetrics]");
    	
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
         
        MetricsRegistry metricRegistery = new MetricsRegistry();
        SignalFxReporter reporter = new SignalFxReporter.Builder(metricRegistery, new StaticAuthToken(auth_token), hostUrlStr)
                .setEndpoint(endpoint)
                .setOnSendErrorHandlerCollection(Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler(){
                	public void handleError(MetricError error){
                		System.out.println("" + error.getMessage());
                	}
                }))
                .setDetailsToAdd(ImmutableSet.of(SignalFxReporter.MetricDetails.COUNT,
                        SignalFxReporter.MetricDetails.MIN, SignalFxReporter.MetricDetails.MAX))
                .build();
        
        MetricName cntName1 = new MetricName("group1", "type1", "counter");
        final MetricMetadata metricMetadata = reporter.getMetricMetadata();
        Counter cnt1 = metricRegistery.newCounter(cntName1);
        metricMetadata.forMetric(cnt1)
                .withMetricName("test1.name")
                .withSourceName("newsource")
                .withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE);

        int count = 0;
        while (true) {
            System.out.println("Sending data: " + cnt1.count());
            Thread.sleep(500);
            	
            cnt1.inc();
            reporter.report();
        }
    }
}
