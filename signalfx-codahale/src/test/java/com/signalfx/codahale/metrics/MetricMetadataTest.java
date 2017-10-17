/**
 * Copyright (C) 2017 SignalFx, Inc.  All rights reserved.
 */
package com.signalfx.codahale.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.signalfx.codahale.reporter.MetricMetadata;
import com.signalfx.codahale.reporter.MetricMetadataImpl;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType;

public class MetricMetadataTest {

    @Test
    public void testRemoveExisting() {
        MetricRegistry metricRegistry = new MetricRegistry();
        MetricMetadata metadata = new MetricMetadataImpl();
        Metric metric = metadata.forBuilder(SettableLongGauge.Builder.INSTANCE)
                .withMetricName("gauge").withDimension("host", "myhost")
                .withMetricType(MetricType.GAUGE).createOrGet(metricRegistry);
        assertFalse(metricRegistry.getMetrics().isEmpty());
        assertTrue(metadata.getMetricType(metric).isPresent());
        assertTrue(metadata.removeMetric(metric, metricRegistry));
        assertFalse(metadata.getMetricType(metric).isPresent());
        assertTrue(metricRegistry.getMetrics().isEmpty());
    }

    @Test
    public void testRemoveMissing() {
        MetricRegistry metricRegistry = new MetricRegistry();
        MetricMetadata metadata = new MetricMetadataImpl();
        Counter counter = metricRegistry.counter("counter");
        assertFalse(metadata.removeMetric(counter, metricRegistry));
    }
}
