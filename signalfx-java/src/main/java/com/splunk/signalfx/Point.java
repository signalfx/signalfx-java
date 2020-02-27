package com.splunk.signalfx;

import java.util.Map;

public abstract class Point {

    private String metric;
    private Map<String, String> dimensions;
    private MetricType type;
    private long timestamp;

    public Point() {
    }

    public Point(String metric, Map<String, String> dimensions, MetricType type, long timestamp) {
        this.metric = metric;
        this.dimensions = dimensions;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Map<String, String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }
}
