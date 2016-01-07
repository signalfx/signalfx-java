/**
 * Copyright (C) 2015 SignalFx, Inc.
 */
package com.signalfx.codahale.reporter;

import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Collection of flags to indicate if a default dimension should be added to a type of metric
 *
 * @author tedo
 *
 */
public class DimensionInclusion {

    public static final short COUNTER = 1 << 0;
    public static final short CUMULATIVE_COUNTER = 1 << 1;
    public static final short GAUGE = 1 << 2;

    public static final short ALL = COUNTER | CUMULATIVE_COUNTER | GAUGE;
    public static final short NOT_COUNTER = CUMULATIVE_COUNTER | GAUGE;

    private final String value;
    private final short inclusion;

    private DimensionInclusion(String value, short inclusion) {
        this.value = value;
        this.inclusion = inclusion;
    }

    public static DimensionInclusion unique(String value) {
        return new DimensionInclusion(value, NOT_COUNTER);
    }

    public static DimensionInclusion shared(String value) {
        return new DimensionInclusion(value, ALL);
    }

    public boolean shouldInclude(SignalFxProtocolBuffers.MetricType metricType) {
        switch (metricType) {
        case GAUGE:
            return checkBit(GAUGE);
        case COUNTER:
            return checkBit(COUNTER);
        case CUMULATIVE_COUNTER:
            return checkBit(CUMULATIVE_COUNTER);
        case ENUM:
        default:
            return false;
        }
    }

    public String getValue() {
        return value;
    }

    private boolean checkBit(short bit) {

        return (inclusion & bit) == bit;
    }
}
