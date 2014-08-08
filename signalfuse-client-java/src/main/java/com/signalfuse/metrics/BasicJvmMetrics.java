package com.signalfuse.metrics;

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

import com.signalfuse.metrics.metric.periodic.DoubleCallback;
import com.signalfuse.metrics.metric.periodic.LongCallback;
import com.signalfuse.metrics.metric.periodic.PeriodicGauge;
import com.signalfuse.metrics.metricbuilder.MetricFactory;

/**
 * Report a basic set of JVM metrics to SignalFuse.
 * 
 * @author psi
 * 
 */
public class BasicJvmMetrics {
    /**
     * Report JVM uptime (milliseconds).
     */
    public final PeriodicGauge uptimeGauge;
    /**
     * Reports total memory used by the JVM heap (bytes).
     */
    public final PeriodicGauge totalMemoryGauge;
    /**
     * Reports current in-use memory in the JVP heap (bytes).
     */
    public final PeriodicGauge usedMemoryGauge;
    /**
     * Reports maximum size of JVM heap (bytes).
     */
    public final PeriodicGauge maxMemoryGauge;
    /**
     * Reports current CPU load (percent, normalized by number of CPUs available to the JVM.
     */
    public final PeriodicGauge cpuLoadGauge;
    /**
     * Reports total number of user and daemon threads.
     */
    public final PeriodicGauge totalThreadCountGauge;
    /**
     * Reports number of daemon threads.
     */
    public final PeriodicGauge daemonThreadCountGauge;
    /**
     * Reports total time spent in garbage collection (nanoseconds).
     */
    public final PeriodicGauge gcTimeGauge;
    /**
     * Reports number of young-generation garbage collections.
     */
    public final PeriodicGauge gcYoungCountGauge;
    /**
     * Reports number of old-generation garbage collections.
     */
    public final PeriodicGauge gcOldCountGauge;
    /**
     * Reports current GC load (percent, normalized by number of CPUs available to the JVM.
     */
    public final PeriodicGauge gcLoadGauge;

    private final RuntimeMXBean runtimeBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final List<GarbageCollectorMXBean> allGcBeans = new ArrayList<GarbageCollectorMXBean>();

    // observed name of the old generation memory pool.
    private static final String OLD_GEN_POOL_NAME = "PS Old Gen";

    /**
     * Construct the basic JVM metrics using a supplied SignalFuse MetricFactory.
     * 
     * @param metricFactory
     */
    public BasicJvmMetrics(MetricFactory metricFactory) {

        runtimeBean = ManagementFactory.getRuntimeMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
        threadBean = ManagementFactory.getThreadMXBean();

        List<GarbageCollectorMXBean> oldGenGcBeans = new ArrayList<GarbageCollectorMXBean>();
        List<GarbageCollectorMXBean> youngGenGcBeans = new ArrayList<GarbageCollectorMXBean>();
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

        this.uptimeGauge = createPeriodicGauge(metricFactory, "jvm.uptime", new UptimeCallback());

        this.totalMemoryGauge = createPeriodicGauge(metricFactory, "jvm.heap.size",
            new TotalMemoryCallback());

        this.usedMemoryGauge = createPeriodicGauge(metricFactory, "jvm.heap.used",
            new UsedMemoryCallback());

        this.maxMemoryGauge = createPeriodicGauge(metricFactory, "jvm.heap.max",
            new MaxMemoryCallback());

        this.cpuLoadGauge = createGauge(metricFactory, "jvm.cpu.load", new CpuLoadCallback());

        this.totalThreadCountGauge = createPeriodicGauge(metricFactory, "jvm.threads.total",
            new TotalThreadCountCallback());

        this.daemonThreadCountGauge = createPeriodicGauge(metricFactory, "jvm.threads.daemon",
            new DaemonThreadCountCallback());

        this.gcTimeGauge = createPeriodicGauge(metricFactory, "jvm.gc.time", new GcTimeCallback());

        this.gcLoadGauge = createGauge(metricFactory, "jvm.gc.load", new GcLoadCallback());

        this.gcYoungCountGauge = createPeriodicGauge(metricFactory, "jvm.gc.young.count",
            new GcCountCallback(youngGenGcBeans));

        this.gcOldCountGauge = createPeriodicGauge(metricFactory, "jvm.gc.old.count",
            new GcCountCallback(oldGenGcBeans));
    }

    private PeriodicGauge createPeriodicGauge(MetricFactory metricFactory, String name,
                                              LongCallback callback) {
        return metricFactory.createPeriodicGauge(metricFactory.createGauge(name), TimeUnit.SECONDS,
            1, callback);
    }

    private PeriodicGauge createGauge(MetricFactory metricFactory, String name,
                                      DoubleCallback callback) {
        return metricFactory.createPeriodicGauge(metricFactory.createGauge(name), TimeUnit.SECONDS,
            1, callback);
    }

    private class UptimeCallback implements LongCallback {
        public long getValue() {
            return runtimeBean.getUptime();
        }
    }

    private class TotalMemoryCallback implements LongCallback {
        public long getValue() {
            return memoryBean.getHeapMemoryUsage().getCommitted();
        }
    }

    private class UsedMemoryCallback implements LongCallback {
        public long getValue() {
            return memoryBean.getHeapMemoryUsage().getUsed();
        }
    }

    private class MaxMemoryCallback implements LongCallback {
        public long getValue() {
            return memoryBean.getHeapMemoryUsage().getMax();
        }
    }

    private class TotalThreadCountCallback implements LongCallback {
        public long getValue() {
            return threadBean.getThreadCount();
        }
    }

    private class DaemonThreadCountCallback implements LongCallback {
        public long getValue() {
            return threadBean.getDaemonThreadCount();
        }
    }

    private class GcTimeCallback implements LongCallback {
        public long getValue() {
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

    private class GcCountCallback implements LongCallback {
        private final List<GarbageCollectorMXBean> gcBeans;

        private GcCountCallback(List<GarbageCollectorMXBean> gcBeans) {
            this.gcBeans = gcBeans;
        }

        public long getValue() {
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

    private abstract class LoadCallback<T> implements DoubleCallback {
        private final int PERCENT = 100;

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

        public double getValue() {
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
