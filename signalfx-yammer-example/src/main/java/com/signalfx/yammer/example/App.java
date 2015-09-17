package com.signalfx.yammer.example;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.lang.Math;
import java.util.concurrent.TimeUnit;

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
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;

import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.SfUtil;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfx.metrics.connection.StoredDataPointReceiver;

import com.signalfx.metrics.errorhandler.MetricError;



public class App {
    
	
	public static void main( String[] args ) throws Exception {
    	
        System.out.println("Running example...");
    
        Properties prop = new Properties();
		prop.load(new FileInputStream("auth.properties"));
		final String auth_token = prop.getProperty("auth");
		final String hostUrlStr = prop.getProperty("host");
		final URL hostUrl = new URL(hostUrlStr);
		System.out.println("Auth=" + auth_token + " .. host=" + hostUrl);
		SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint(hostUrl.getProtocol(), hostUrl.getHost(),
				hostUrl.getPort());

		MetricsRegistry metricRegistery = new MetricsRegistry();
		SignalFxReporter reporter = new SignalFxReporter.Builder(metricRegistery, new StaticAuthToken(auth_token),
				hostUrlStr).setEndpoint(endpoint)
						.setOnSendErrorHandlerCollection(
								Collections.<OnSendErrorHandler> singleton(new OnSendErrorHandler() {
									public void handleError(MetricError error) {
										System.out.println("" + error.getMessage());
									}
								}))
						.setDetailsToAdd(ImmutableSet.of(SignalFxReporter.MetricDetails.COUNT,
								SignalFxReporter.MetricDetails.MIN, SignalFxReporter.MetricDetails.MAX))
						.build();

		final MetricMetadata metricMetadata = reporter.getMetricMetadata();
		
		// Counter
		
		MetricName counterName = new MetricName("group1", "type1", "counter");
		Counter counter = metricRegistery.newCounter(counterName);
		metricMetadata.forMetric(counter)
				.withMetricName("Test-Metric")
				.withSourceName("Test-Source")
				.withDimension("Test-Dimension", "_Counter")
				.withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE);
		
		
		// Cumulative Counter
		
		MetricName counterCallbackName = new MetricName("group1", "type1", "cumulative_counter_callback");
        Metric cumulativeCounter = SfUtil.cumulativeCounter(
        		metricRegistery, 
        		counterCallbackName, 
        		metricMetadata, 
        		new Gauge<Long>() {
    
        			private long i = 0;
        			
        			@Override public Long value() {            
        				return i++;
        			}
        
        		});
        
		metricMetadata.forMetric(cumulativeCounter)
			.withMetricName("Test-Metric")
			.withSourceName("Test-Source")
			.withDimension("Test-Dimension", "_CumulativeCounter");
		
		// Gauge
		
		MetricName gaugeName = new MetricName("group1", "type1", "gauge");
		Gauge gauge = metricRegistery.newGauge(gaugeName, new Gauge<Double>() {
            @Override
            public Double value() {
                return Math.sin(System.currentTimeMillis() * 0.001 * 2 * Math.PI / 60);
            }
        });
		
		metricMetadata.forMetric(gauge)
				.withMetricName("Test-Metric")
				.withSourceName("Test-Source")
				.withDimension("Test-Dimension", "_Gauge")
				.withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE);
		
		// Timer
		
		MetricName timerName = new MetricName("group1", "type1", "timer");
		Timer timer = metricRegistery.newTimer(timerName, TimeUnit.SECONDS, TimeUnit.SECONDS);
		
		metricMetadata.forMetric(timer)
				.withMetricName("Test-Metric") // To see metric for the timer you should look for
											   // Test-Metric.max / Test-Metric.min
				.withSourceName("Test-Source")
				.withDimension("Test-Dimension", "_Timer")
				.withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE);

		while (true) {
			
			TimerContext timerContext = timer.time(); // Start timer
			
			System.out.println("Sending data...");
			Thread.sleep(500);
			counter.inc();
			
			timerContext.stop(); // Stop timer
			
			reporter.report(); // Report all metrics 
		}
	
	}
	
}