package com.signalfuse.metrics.metric.internal;

import com.signalfuse.metrics.metric.Metric;

/**
 * @author jack
 */
public abstract class BaseMetric implements Metric {
    private final String source;
    private final String metric;

    protected BaseMetric(Metric metric) {
        this(metric.getSource(), metric.getMetric());
    }

    protected BaseMetric(String source, String metric) {
        this.source = source;
        this.metric = metric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Metric)) {
            return false;
        }

        Metric hashPair = (Metric) o;

        if (!metric.equals(hashPair.getMetric())) {
            return false;
        }
        if (!source.equals(hashPair.getSource())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + metric.hashCode();
        return result;
    }

    @Override public String getSource() {
        return source;
    }

    @Override public String getMetric() {
        return metric;
    }
}
