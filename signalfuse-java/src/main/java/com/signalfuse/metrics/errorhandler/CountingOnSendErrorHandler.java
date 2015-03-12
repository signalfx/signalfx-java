package com.signalfx.metrics.errorhandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.ImmutableMap;

/**
 * Counts errors.
 */
public class CountingOnSendErrorHandler implements OnSendErrorHandler {
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    private final Map<MetricErrorType, AtomicInteger> vals = new ConcurrentHashMap<MetricErrorType, AtomicInteger>();

    @Override
    public void handleError(MetricError metricError) {
        totalErrors.incrementAndGet();
        AtomicInteger existingValue = vals.get(metricError.getMetricErrorType());
        if (existingValue != null) {
            existingValue.incrementAndGet();
        } else {
            synchronized (this) {
                AtomicInteger previousValue = new AtomicInteger(1);
                AtomicInteger prevValue = vals.put(metricError.getMetricErrorType(), previousValue);
                if (prevValue != null) {
                    previousValue.addAndGet(prevValue.get());
                }
            }
        }
    }

    public synchronized Map<MetricErrorType, AtomicInteger> getValues() {
        return ImmutableMap.copyOf(vals);
    }

    public int getTotalErrorCount() {
        return totalErrors.get();
    }
}
