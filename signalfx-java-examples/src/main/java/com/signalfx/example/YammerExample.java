package com.signalfx.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.signalfx.codahale.reporter.SfUtil;
import com.signalfx.codahale.reporter.SignalFxReporter;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

// An example class for Yammer 2.x metrics.  For more information see
// http://metrics.dropwizard.io/2.2.0/getting-started/

public class YammerExample {

    private static String LIBRARY_VERSION = "library-version";
    private static String YAMMER = "yammer";

    public static void main(String[] args) throws Exception {
        MetricsRegistry registry = new MetricsRegistry();
        SignalFxReporter reporter = buildReporter(registry);
        registerCumulativeCounter(registry, reporter);
        registerGauge(registry, reporter);
        Counter counter = registerCounter(registry, reporter);
        Timer timer = registerTimer(registry, reporter);
        while (true) {
            TimerContext timerContext = timer.time();
            try {
                Thread.sleep(500);
                counter.inc();
            } finally {
                timerContext.stop();
            }
            System.out.println("Reporting");
            reporter.report(); // Report all metrics
        }
    }

    private static SignalFxReporter buildReporter(MetricsRegistry metricsRegistry) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("auth.properties"));
        String token = prop.getProperty("token");
        String urlStr = prop.getProperty("host");

        URL url = new URL(urlStr);
        System.out.println("token=" + token + " host=" + url);
        SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint(
                url.getProtocol(),
                url.getHost(),
                url.getPort()
        );
        return new SignalFxReporter.Builder(metricsRegistry, new StaticAuthToken(token), urlStr)
                .setEndpoint(endpoint)
                .setOnSendErrorHandlerCollection(Collections.singleton(error -> System.out.println(error.getMessage())))
                .setDetailsToAdd(ImmutableSet.of(
                        SignalFxReporter.MetricDetails.COUNT,
                        SignalFxReporter.MetricDetails.MIN,
                        SignalFxReporter.MetricDetails.MAX
                ))
                .build();
    }

    private static void registerCumulativeCounter(MetricsRegistry metricsRegistry, SignalFxReporter reporter) {
        MetricName cumulativeCounterName = new MetricName(YammerExample.class, "yammer.test.cumulativeCounter");
        Metric cumulativeCounter = SfUtil.cumulativeCounter(
                metricsRegistry,
                cumulativeCounterName,
                reporter,
                new Gauge<Long>() {
                    long i = 0;
                    @Override
                    public Long value() {
                        return i++;
                    }

                }
        );
        reporter.setDimension(cumulativeCounter, LIBRARY_VERSION, YAMMER);
    }

    private static void registerGauge(MetricsRegistry metricsRegistry, SignalFxReporter reporter) {
        Gauge<Double> gauge = metricsRegistry.newGauge(YammerExample.class, "yammer.test.gauge",
                new Gauge<Double>() {
                    @Override
                    public Double value() {
                        return Math.sin(System.currentTimeMillis() * 0.001 * 2 * Math.PI / 60);
                    }
                }
        );
        reporter.setDimension(gauge, LIBRARY_VERSION, YAMMER);
    }

    private static Counter registerCounter(MetricsRegistry metricsRegistry, SignalFxReporter reporter) {
        Counter counter = metricsRegistry.newCounter(YammerExample.class, "yammer.test.counter");
        reporter.setDimension(counter, LIBRARY_VERSION, YAMMER);
        return counter;
    }

    private static Timer registerTimer(MetricsRegistry metricsRegistry, SignalFxReporter reporter) {
        Timer timer = metricsRegistry.newTimer(YammerExample.class, "yammer.test.timer", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        reporter.setDimension(timer, LIBRARY_VERSION, YAMMER);
        return timer;
    }
}
