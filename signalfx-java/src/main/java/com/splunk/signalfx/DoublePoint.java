package com.splunk.signalfx;

import java.util.Map;

public class DoublePoint extends Point {

    private double value;

    public DoublePoint() {
    }

    public DoublePoint(String metric, Map<String, String> dimensions, MetricType type, long timestamp, double value) {
        super(metric, dimensions, type, timestamp);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
