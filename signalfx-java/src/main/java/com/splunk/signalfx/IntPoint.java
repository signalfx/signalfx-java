package com.splunk.signalfx;

import java.util.Map;

class IntPoint extends Point {

    private int value;

    public IntPoint() {
    }

    public IntPoint(String metric, Map<String, String> dimensions, MetricType type, long timestamp, int value) {
        super(metric, dimensions, type, timestamp);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
