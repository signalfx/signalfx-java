package com.signalfuse.metrics.metric;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.signalfuse.metrics.MetricFactoryBuilder;
import com.signalfuse.metrics.connection.InjectedDataPointReceiverFactory;
import com.signalfuse.metrics.connection.StoredDataPointReceiver;
import com.signalfuse.metrics.datumhandler.DirectCallDatumHandler;
import com.signalfuse.metrics.metricbuilder.MetricFactory;
import com.signalfuse.metrics.metricbuilder.errorhandler.CountingOnSendErrorHandler;

public class ErrorsTest {
    private MetricFactoryBuilder builder;
    private CountingOnSendErrorHandler counter;
    private StoredDataPointReceiver dbank;

    @Before
    public void init() {
        dbank = new StoredDataPointReceiver();
        counter = new CountingOnSendErrorHandler();
        builder = new MetricFactoryBuilder().usingToken("").addSendErrorHandler(counter)
                                            .usingDatumHandlerFactory(
                                                    new DirectCallDatumHandler.Factory())
                                            .usingDataPointReceiverFactory(
                                                    new InjectedDataPointReceiverFactory(dbank));
    }

    @Test
    public void testFailure() {
        MetricFactory metricFactory = builder.build();
        assertEquals(0, counter.getTotalErrorCount());
        metricFactory.createGauge("test").value(10);
        assertEquals(0, counter.getTotalErrorCount());
        dbank.throwOnAdd = true;
        metricFactory.createGauge("test").value(10);
        assertEquals(1, counter.getTotalErrorCount());
    }
}
