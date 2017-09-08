package com.signalfx.codahale.metrics;

import static org.junit.Assert.*;

import org.junit.Test;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.Histogram;
import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.SfUtil;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.StaticDataPointReceiverFactory;
import com.signalfx.metrics.connection.StoredDataPointReceiver;
import com.signalfx.metrics.errorhandler.MetricError;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SignalFxReporterTest {
	
	private void testReporter(){
		StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());
        
        MetricsRegistry metricRegistery = new MetricsRegistry();
        SignalFxReporter reporter = new SignalFxReporter.Builder(metricRegistery, new StaticAuthToken(""), "myserver")
                .setDataPointReceiverFactory(new StaticDataPointReceiverFactory(dbank))
                .setDetailsToAdd(
            		ImmutableSet.of(
        				SignalFxReporter.MetricDetails.COUNT,
        				SignalFxReporter.MetricDetails.MIN, 
        				SignalFxReporter.MetricDetails.MAX
            		)
                )
                .setName("testReporter")
                .useLocalTime(false)
                .sendExtraMetricDimensions(true)
                .setOnSendErrorHandlerCollection(
                		Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler(){
                        	public void handleError(MetricError error){
                        		System.out.println("" + error.getMessage());
                        	}
                        })
                )
                .setFilter(MetricPredicate.ALL)
                .setRateUnit(TimeUnit.SECONDS)
                .build();
        
        final MetricMetadata metricMetadata = reporter.getMetricMetadata();

        
        MetricName gaugeName = new MetricName("group1", "type1", "gauge1");
        metricRegistery.newGauge(gaugeName, new Gauge<Integer>() {
            @Override
            public Integer value() {
                return 1;
            }
        });
        
        MetricName boolGaugeName = new MetricName("group1", "type1", "boolGauge1");
        metricRegistery.newGauge(boolGaugeName, new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return true;
            }
        });

        MetricName cntName1 = new MetricName("group1", "type1", "counter");
        Counter cnt1 = metricRegistery.newCounter(cntName1);
        metricMetadata.forMetric(cnt1)
                .withMetricName("newname")
                .withSourceName("newsource")
                .withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                .withDimension("key", "value");
        
        MetricName cntName2 = new MetricName("group1", "type1", "counter2");
        Counter cnt2 = metricRegistery.newCounter(cntName2);
        metricMetadata.forMetric(cnt2)
                .withMetricName("newname2");
        cnt1.inc();
        cnt1.inc();

        MetricName timerName1 = new MetricName("group1", "type1", "atimer");
        metricRegistery.newTimer(timerName1, TimeUnit.SECONDS, TimeUnit.SECONDS).time().stop();

        reporter.report();
        
        assertEquals(7, dbank.addDataPoints.size());
        assertEquals("newname", dbank.addDataPoints.get(2).getMetric());
        assertEquals("newsource", dbank.addDataPoints.get(2).getDimensions(0).getValue());
        assertEquals("sf_source", dbank.addDataPoints.get(2).getDimensions(0).getKey());
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get(
                "newname"));
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER, dbank.registeredMetrics.get(
                "atimer.count"));
        assertEquals(SignalFxProtocolBuffers.MetricType.GAUGE, dbank.registeredMetrics.get(
                "atimer.max"));
        assertEquals(2, dbank.lastValueFor("newsource", "newname").getIntValue());
        assertEquals("metric_group", dbank.addDataPoints.get(2).getDimensions(2).getKey());
        assertEquals("group1", dbank.addDataPoints.get(2).getDimensions(2).getValue());
        assertEquals("type1", dbank.addDataPoints.get(2).getDimensions(3).getValue());

        assertNotNull(dbank.lastValueFor("myserver", "atimer.count"));

        dbank.addDataPoints.clear();
        
        MetricName rawCounterName = new MetricName("group1", "type1", "raw_counter");
        Counter rawCounter = metricRegistery.newCounter(rawCounterName);
        MetricName counterCallbackName = new MetricName("group1", "type1", "cumulative_counter_callback");
        metricMetadata.forMetric(rawCounter)
                .withMetricType(SignalFxProtocolBuffers.MetricType.COUNTER);
        SfUtil.cumulativeCounter(metricRegistery, counterCallbackName, metricMetadata, new Gauge<Long>() {
            private long i = 0;

            @Override public Long value() {
                return i++;
            }
        });

        rawCounter.inc(10);

        reporter.report();
        assertEquals(9, dbank.addDataPoints.size());
        assertEquals(10, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(0, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());

        assertEquals(2, dbank.lastValueFor("newsource", "newname").getIntValue());

        assertEquals(SignalFxProtocolBuffers.MetricType.COUNTER, dbank.registeredMetrics.get("raw_counter"));
        assertEquals(SignalFxProtocolBuffers.MetricType.CUMULATIVE_COUNTER,
                dbank.registeredMetrics.get("cumulative_counter_callback"));
        rawCounter.inc(14);
        dbank.addDataPoints.clear();

        metricRegistery.removeMetric(cntName1);
        assertEquals(true, dbank.clearValues("newsource", "newname"));
        assertEquals(false, dbank.clearValues("newsource", "newname")); 
        reporter.report();
        
        assertEquals(8, dbank.addDataPoints.size());
        assertEquals(0, dbank.valuesFor("newsource", "newname").size());
        assertEquals(24, dbank.lastValueFor("myserver", "raw_counter").getIntValue());
        assertEquals(1, dbank.lastValueFor("myserver", "cumulative_counter_callback").getIntValue());
        
        
        
        long endTime = System.currentTimeMillis() + 1000 * 10;
        reporter.start(1, TimeUnit.SECONDS);
        
        int size = 0;
        while(endTime > System.currentTimeMillis()){
        	if(size != dbank.addDataPoints.size()){
	        	break;
        	}
        }
        
        assertTrue(endTime > System.currentTimeMillis());
        
        reporter.stop();
	}
	
	private void testReporterWithDetails(){
		
		StoredDataPointReceiver dbank = new StoredDataPointReceiver();
        assertEquals(0, dbank.addDataPoints.size());

        Set<SignalFxReporter.MetricDetails> detailsToAdd = new HashSet<SignalFxReporter.MetricDetails>();
        detailsToAdd.add(SignalFxReporter.MetricDetails.STD_DEV);
        detailsToAdd.add(SignalFxReporter.MetricDetails.MEAN);
        
        MetricsRegistry metricRegistery = new MetricsRegistry();
        SignalFxReporter reporter = new SignalFxReporter.Builder(metricRegistery, new StaticAuthToken(""), "myserver")
                .setDataPointReceiverFactory(new StaticDataPointReceiverFactory(dbank))
                .setDetailsToAdd(
            		ImmutableSet.of(
        				SignalFxReporter.MetricDetails.COUNT,
        				SignalFxReporter.MetricDetails.MIN, 
        				SignalFxReporter.MetricDetails.MAX
            		)
                )
                .setName("testReporter")
                .setDefaultSourceName("defaultSource")
                .useLocalTime(false)
                .setOnSendErrorHandlerCollection(
                		Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler(){
                        	public void handleError(MetricError error){
                        		System.out.println("" + error.getMessage());
                        	}
                        })
                )
                .setFilter(MetricPredicate.ALL)
                .setRateUnit(TimeUnit.SECONDS)
                .setDetailsToAdd(detailsToAdd)
                .build();
        
        final MetricMetadata metricMetadata = reporter.getMetricMetadata();
        
        MetricName histogramName = new MetricName("group1", "type1", "histogram");
        Histogram histogram = metricRegistery.newHistogram(histogramName, true);
        histogram.update(10);
        histogram.update(14);
        histogram.update(7);
        
        metricMetadata.forMetric(histogram)
        	.withMetricName("histogram")
        	.withSourceName("histogram_source")
        	.withMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
        	.withDimension("key", "value");
        
        reporter.report();
        
        assertEquals(2, dbank.addDataPoints.size());
		
	}
	
    @Test
    public void test() throws InterruptedException {
        testReporter();
        testReporterWithDetails();
    }
}
