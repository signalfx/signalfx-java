package com.signalfx.example;

import java.io.FileInputStream;
import java.lang.String;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.SfUtil;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.errorhandler.MetricError;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/*
    An example class for Yammer 2.x metrics.  For more information see
    http://metrics.dropwizard.io/2.2.0/getting-started/
 */
public class YammerExample {

    public static final String SIGNAL_FX = "SignalFx";
    public static final String LIBRARY_VERSION = "library-version";
    public static final String YAMMER = "yammer";

    public static void main(String[] args) throws Exception {

        System.out.println("Running example...");

        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));
        final String auth_token = prop.getProperty("auth");
        final String hostUrlStr = prop.getProperty("host");
        final URL hostUrl = new URL(hostUrlStr);
        System.out.println("Auth=" + auth_token + " .. host=" + hostUrl);
        SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint(hostUrl.getProtocol(),
                hostUrl.getHost(), hostUrl.getPort());

        MetricsRegistry metricsRegistry = new MetricsRegistry();
        SignalFxReporter reporter = new SignalFxReporter.Builder(metricsRegistry,
                new StaticAuthToken(auth_token),
                hostUrlStr).setEndpoint(endpoint)
                .setOnSendErrorHandlerCollection(
                        Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler() {
                            public void handleError(MetricError error) {
                                System.out.println("" + error.getMessage());
                            }
                        }))
                .setDetailsToAdd(ImmutableSet.of(SignalFxReporter.MetricDetails.COUNT,
                        SignalFxReporter.MetricDetails.MIN,
                        SignalFxReporter.MetricDetails.MAX))
                .build();

        final MetricMetadata metricMetadata = reporter.getMetricMetadata();

        Counter counter = getCounter(metricsRegistry, metricMetadata);

        Metric cumulativeCounter = getCumulativeCounter(metricsRegistry, metricMetadata);

        Gauge gauge1 = getGauge(metricsRegistry, metricMetadata);

        Timer timer = getTimer(metricsRegistry, metricMetadata);

        // main body generating data and sending it in a loop
        while (true) {
            final TimerContext context = timer.time();
            try {
                System.out.println("Sending data...");
                Thread.sleep(500);
                counter.inc();
            } finally {
                context.stop();
            }
            reporter.report(); // Report all metrics
        }

    }

    private static Counter getCounter(MetricsRegistry metricsRegistry,
                                      MetricMetadata metricMetadata) {
        Counter counter = metricsRegistry.newCounter(YammerExample.class, "yammer.test.counter");
        metricMetadata.forMetric(counter)
                .withSourceName("signalFx")
                .withDimension(LIBRARY_VERSION, YAMMER);
        return counter;
    }

    /*
      There will be 3 metrics present:
      yammer.test.timer.count  # cumulative counter
      yammer.test.timer.max    # gauge
      yammer.test.timer.min    # gauge
     */
    private static Timer getTimer(MetricsRegistry metricsRegistry, MetricMetadata metricMetadata) {
        Timer timer = metricsRegistry
                .newTimer(YammerExample.class, "yammer.test.timer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        metricMetadata.forMetric(timer)
                .withSourceName(SIGNAL_FX)
                .withDimension(LIBRARY_VERSION, YAMMER);
        return timer;
    }

    private static Gauge getGauge(MetricsRegistry metricsRegistry, MetricMetadata metricMetadata) {
        Gauge gauge = metricsRegistry.newGauge(YammerExample.class, "yammer.test.gauge",
                new Gauge<Double>() {
                    @Override
                    public Double value() {
                        return Math.sin(System.currentTimeMillis() * 0.001 * 2 * Math.PI / 60);
                    }
                });

        metricMetadata.forMetric(gauge)
                .withSourceName(SIGNAL_FX)
                .withDimension(LIBRARY_VERSION, YAMMER);
        return gauge;
    }

    private static Metric getCumulativeCounter(MetricsRegistry metricsRegistry,
                                               MetricMetadata metricMetadata) {
        MetricName counterCallbackName = new MetricName(YammerExample.class, "yammer.test.cumulativeCounter");
        Metric cumulativeCounter = SfUtil.cumulativeCounter(
                metricsRegistry,
                counterCallbackName,
                metricMetadata,
                new Gauge<Long>() {

                    private long i = 0;

                    @Override
                    public Long value() {
                        return i++;
                    }

                });

        metricMetadata.forMetric(cumulativeCounter)
                .withSourceName(SIGNAL_FX)
                .withDimension(LIBRARY_VERSION, YAMMER);

        return cumulativeCounter;
    }

}