/**
 * Copyright (C) 2014-2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.codahale.metrics.util;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.signalfx.codahale.util.BasicJvmMetrics;

public class BasicJvmMetrisTest {

    @Test
    public void testPointsSent() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        new BasicJvmMetrics(registry);

        ScheduledReporter reporter = new ScheduledReporter(registry, "test", MetricFilter.ALL,
                TimeUnit.SECONDS, TimeUnit.MILLISECONDS) {

            @Override
            public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                               SortedMap<String, Histogram> histograms,
                               SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
                Assert.assertFalse(gauges.isEmpty());
                Assert.assertNotNull(gauges.get("jvm.uptime"));
                for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                    Assert.assertNotNull(entry.getValue().getValue());
                }
            }
        };

        reporter.report();
        reporter.close();
    }
}
