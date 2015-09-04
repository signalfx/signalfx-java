package com.signalfx.codahale.reporter;

import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Metric;

/**
 * The abstract base class for all scheduled reporters (i.e., reporters which process a registry's
 * metrics periodically).
 *
 */
public abstract class CustomScheduledReporter {
    /**
     * A simple named thread factory.
     */
    @SuppressWarnings("NullableProblems")
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private NamedThreadFactory(String name) {
            final SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = "metrics-" + name + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private final MetricsRegistry registry;
    private final ScheduledExecutorService executor;
    private final MetricPredicate filter;

    /**
     * Creates a new {@link CustomScheduledReporter} instance.
     *
     * @param registry 		the MetricsRegistry containing the metrics this
     *                 		reporter will report
     * @param name     		the reporter's name
     * @param filter   		the filter for which metrics to report
     * @param rateUnit   	
     * @param durationUnit  
     */
    protected CustomScheduledReporter(MetricsRegistry registry,
                                		String name,
                                		MetricPredicate filter,
                                		TimeUnit rateUnit,
                                		TimeUnit durationUnit) {
        this.registry = registry;
        this.filter = filter;
        this.executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(name));
    }
    
    /**
     * get Metrics by class and predicate
     * 
     * @param klass
     * @param filter
     * @return
     */
    
    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricName, T> getMetrics(Class<T> klass, MetricPredicate filter) {
    	
    	Map<MetricName, Metric> allMetrics = registry.allMetrics();
    	final TreeMap<MetricName, T> timers = new TreeMap<MetricName, T>();
    	
		for (Map.Entry<MetricName, Metric> entry : allMetrics.entrySet()) {
			if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(),
                                                                     entry.getValue())) {
                timers.put(entry.getKey(), (T) entry.getValue());
			}
		}
		
		return Collections.unmodifiableSortedMap(timers);
    }

    /**
     * get all Gauge metrics
     * @param filter
     * @return
     */

    private SortedMap<MetricName, Gauge> getGauges(MetricPredicate filter) {
    	return getMetrics(Gauge.class, filter);
    }
    
    /**
     * get all Counter metrics
     * @param filter
     * @return
     */
    
    private SortedMap<MetricName, Counter> getCounters(MetricPredicate filter) {
    	return getMetrics(Counter.class, filter);
    }
    
    /**
     * get all Histogram metrics
     * @param filter
     * @return
     */
    
    private SortedMap<MetricName, Histogram> getHistograms(MetricPredicate filter) {
    	return getMetrics(Histogram.class, filter);
    }
    
    /**
     * get all Meters metrics
     * @param filter
     * @return
     */
    
    private SortedMap<MetricName, Meter> getMeters(MetricPredicate filter) {
    	return getMetrics(Meter.class, filter);
    }
    
    /**
     * get all Timers metrics
     * @param filter
     * @return
     */
    
    private SortedMap<MetricName, Timer> getTimers(MetricPredicate filter) {
    	return getMetrics(Timer.class, filter);
    }
    
    /**
     * Starts the reporter polling at the given period.
     *
     * @param period the amount of time between polls
     * @param unit   the unit for {@code period}
     */
    public void start(long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                report();
            }
        }, period, period, unit);
    }

    /**
     * Stops the reporter and shuts down its thread of execution.
     */
    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            // do nothing
        }
    }

    /**
     * Report the current values of all metrics in the registry.
     */
    public void report() {
        report(getGauges(filter),
               getCounters(filter),
               getHistograms(filter),
               getMeters(filter),
               getTimers(filter));
    }
    
    /**
     * Called periodically by the polling thread. Subclasses should report all the given metrics.
     *
     * @param gauges     all of the gauges in the registry
     * @param counters   all of the counters in the registry
     * @param histograms all of the histograms in the registry
     * @param meters     all of the meters in the registry
     * @param timers     all of the timers in the registry
     */
    public abstract void report(SortedMap<MetricName, Gauge> gauges,
                                SortedMap<MetricName, Counter> counters,
                                SortedMap<MetricName, Histogram> histograms,
                                SortedMap<MetricName, Meter> meters,
                                SortedMap<MetricName, Timer> timers);

}
