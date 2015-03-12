/**
 * Copyright (C) 2014 SignalFx, Inc.
 */
package com.signalfx.codahale.util;


import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;


/**
 * Report a basic set of JVM metrics to SignalFx.
 *
 * @author psi
 *
 */
public class BasicJvmMetrics {
    /**
     * Report JVM uptime (milliseconds).
     */
    public final Gauge<Long> uptimeGauge;
    /**
     * Reports total memory used by the JVM heap (bytes).
     */
    public final Gauge<Long> totalMemoryGauge;
    /**
     * Reports current in-use memory in the JVP heap (bytes).
     */
    public final Gauge<Long> usedMemoryGauge;
    /**
     * Reports maximum size of JVM heap (bytes).
     */
    public final Gauge<Long> maxMemoryGauge;
    /**
     * Reports current CPU load (percent, normalized by number of CPUs available to the JVM.
     */
    public final Gauge<Double> cpuLoadGauge;
    /**
     * Reports total number of user and daemon threads.
     */
    public final Gauge<Integer> totalThreadCountGauge;
    /**
     * Reports number of daemon threads.
     */
    public final Gauge<Integer> daemonThreadCountGauge;
    /**
     * Reports total time spent in garbage collection (nanoseconds).
     */
    public final Gauge<Long> gcTimeGauge;
    /**
     * Reports number of young-generation garbage collections.
     */
    public final Gauge<Long> gcYoungCountGauge;
    /**
     * Reports number of old-generation garbage collections.
     */
    public final Gauge<Long> gcOldCountGauge;
    /**
     * Reports current GC load (percent, normalized by number of CPUs available to the JVM.
     */
    public final Gauge<Double> gcLoadGauge;

    private final RuntimeMXBean runtimeBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final List<GarbageCollectorMXBean> oldGenGcBeans = new ArrayList<GarbageCollectorMXBean>();
    private final List<GarbageCollectorMXBean> youngGenGcBeans = new ArrayList<GarbageCollectorMXBean>();
    private final List<GarbageCollectorMXBean> allGcBeans = new ArrayList<GarbageCollectorMXBean>();

    // observed name of the old generation memory pool.
    private static final String OLD_GEN_POOL_NAME = "PS Old Gen";

    /**
     * Construct the basic JVM metrics using a supplied SignalFx MetricFactory.
     *
     * @param metricRegistry The registry to give these metrics to
     */
    public BasicJvmMetrics(MetricRegistry metricRegistry) {

        runtimeBean = ManagementFactory.getRuntimeMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
        threadBean = ManagementFactory.getThreadMXBean();

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            allGcBeans.add(gcBean);

            Set<String> poolNames = new HashSet<String>(Arrays.asList(gcBean.getMemoryPoolNames()));

            if (poolNames.contains(OLD_GEN_POOL_NAME)) {
                // We'll count garbage collectors managing the OLD_GEN_POOL_NAME as 'old generation'
                oldGenGcBeans.add(gcBean);
            } else {
                // and all others as 'young generation'
                youngGenGcBeans.add(gcBean);
            }
        }

        this.uptimeGauge = createPeriodicGauge(metricRegistry, "jvm.uptime", new UptimeCallback());

        this.totalMemoryGauge = createPeriodicGauge(metricRegistry, "jvm.heap.size",
            new TotalMemoryCallback());

        this.usedMemoryGauge = createPeriodicGauge(metricRegistry, "jvm.heap.used",
            new UsedMemoryCallback());

        this.maxMemoryGauge = createPeriodicGauge(metricRegistry, "jvm.heap.max",
            new MaxMemoryCallback());

        this.cpuLoadGauge = createDoublePeriodicGauge(metricRegistry, "jvm.cpu.load", new CpuLoadCallback());

        this.totalThreadCountGauge = createIntegerPeriodicGauge(metricRegistry, "jvm.threads.total",
            new TotalThreadCountCallback());

        this.daemonThreadCountGauge = createIntegerPeriodicGauge(metricRegistry, "jvm.threads.daemon",
            new DaemonThreadCountCallback());

        this.gcTimeGauge = createPeriodicGauge(metricRegistry, "jvm.gc.time", new GcTimeCallback());

        this.gcLoadGauge = createDoublePeriodicGauge(metricRegistry, "jvm.gc.load", new GcLoadCallback());

        this.gcYoungCountGauge = createPeriodicGauge(metricRegistry, "jvm.gc.young.count",
            new GcCountCallback(youngGenGcBeans));

        this.gcOldCountGauge = createPeriodicGauge(metricRegistry, "jvm.gc.old.count",
            new GcCountCallback(oldGenGcBeans));
    }

    private Gauge<Long> createPeriodicGauge(MetricRegistry metricRegistry, String name,
                                              Gauge<Long> gauge) {
        return metricRegistry.register(name, gauge);
    }

    private Gauge<Integer> createIntegerPeriodicGauge(MetricRegistry metricRegistry, String name,
            Gauge<Integer> gauge) {
        return metricRegistry.register(name, gauge);
    }

    private Gauge<Double> createDoublePeriodicGauge(MetricRegistry metricRegistry, String name,
                                              Gauge<Double> gauge) {
        return metricRegistry.register(name, gauge);
    }

    private class UptimeCallback implements Gauge<Long> {
        @Override
        public Long getValue() {
            return runtimeBean.getUptime();
        }
    }

    private class TotalMemoryCallback implements Gauge<Long> {
        @Override
        public Long getValue() {
            return memoryBean.getHeapMemoryUsage().getCommitted();
        }
    }

    private class UsedMemoryCallback implements Gauge<Long> {
        @Override
        public Long getValue() {
            return memoryBean.getHeapMemoryUsage().getUsed();
        }
    }

    private class MaxMemoryCallback implements Gauge<Long> {
        @Override
        public Long getValue() {
            return memoryBean.getHeapMemoryUsage().getMax();
        }
    }

    private class TotalThreadCountCallback implements Gauge<Integer> {
        @Override
        public Integer getValue() {
            return threadBean.getThreadCount();
        }
    }

    private class DaemonThreadCountCallback implements Gauge<Integer> {
        @Override
        public Integer getValue() {
            return threadBean.getDaemonThreadCount();
        }
    }

    private class GcTimeCallback implements Gauge<Long> {
        @Override
        public Long getValue() {
            long total = 0;
            for (GarbageCollectorMXBean gcBean : allGcBeans) {
                long sample = gcBean.getCollectionTime();
                if (sample > 0) {
                    total += sample;
                }
            }
            return total;
        }
    }

    private static class GcCountCallback implements Gauge<Long> {
        final private List<GarbageCollectorMXBean> gcBeans;

        private GcCountCallback(List<GarbageCollectorMXBean> gcBeans) {
            this.gcBeans = gcBeans;
        }

        @Override
        public Long getValue() {
            long total = 0;
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                long sample = gcBean.getCollectionCount();
                if (sample > 0) {
                    total += sample;
                }
            }
            return total;
        }
    }

    private abstract class LoadCallback<T> implements Gauge<Double> {
        private static final int PERCENT = 100;

        private long previousTime;
        private double previousValue;
        private Map<T, Long> samples = new HashMap<T, Long>();
        private final TimeUnit timeUnit;

        LoadCallback(TimeUnit timeUnit) {
            this.previousTime = 0;
            this.previousValue = 0;
            this.timeUnit = timeUnit;
        }

        protected abstract Map<T, Long> getSamples();

        private long computeDelta(Map<T, Long> newSamples) {
            long delta = 0;

            for (Map.Entry<T, Long> entry : newSamples.entrySet()) {
                T key = entry.getKey();
                Long sample = entry.getValue();
                if (sample < 0) {
                    // not valid for this key
                } else {
                    Long previous = samples.get(key);
                    if (previous == null) {
                        delta += sample;  // first sample
                    } else {
                        delta += sample - previous;
                    }
                }
            }
            samples = newSamples;

            return delta;
        }

        @Override
        public Double getValue() {
            long time = runtimeBean.getUptime();
            long deltaTime = time - previousTime;

            if (deltaTime < 100) {
                return previousValue;
            }

            Map<T, Long> samples = getSamples();

            long deltaLoad = computeDelta(samples);

            previousValue = (double) PERCENT * timeUnit.toNanos(deltaLoad)
                / TimeUnit.MILLISECONDS.toNanos(deltaTime)
                / Runtime.getRuntime().availableProcessors();

            previousTime = time;

            return previousValue;
        }
    }

    // com.sun.management.OperatingSystemMXBean has a getProcessCpuTime()
    // but java.lang.management.OperatingSystemMXBean does not
    private class CpuLoadCallback extends LoadCallback<Long> {
        CpuLoadCallback() {
            super(TimeUnit.NANOSECONDS);
        }

        @Override
        protected Map<Long, Long> getSamples() {
            Map<Long, Long> samples = new HashMap<Long, Long>();
            for (long threadId : threadBean.getAllThreadIds()) {
                samples.put(threadId, threadBean.getThreadCpuTime(threadId));
            }
            return samples;
        }
    }

    // factor stuff out of this and CpuLoadCallback
    private class GcLoadCallback extends LoadCallback<String> {
        GcLoadCallback() {
            super(TimeUnit.MILLISECONDS);
        }

        @Override
        protected Map<String, Long> getSamples() {
            Map<String, Long> samples = new HashMap<String, Long>();
            for (GarbageCollectorMXBean gcBean : allGcBeans) {
                samples.put(gcBean.getName(), gcBean.getCollectionTime());
            }
            return samples;
        }
    }
}
