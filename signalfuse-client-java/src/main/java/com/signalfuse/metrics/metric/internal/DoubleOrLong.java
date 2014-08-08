package com.signalfuse.metrics.metric.internal;

/**
 * <p>Stores either a double or long and smartly switched between them.</p>
 * <p>
 * Tries to keep them as a long, but once you do a double operation on the value, it is converted to
 * a double until cleared.
 * </p>
 * @author jack
 */
public class DoubleOrLong {
    private long currentLong;
    private double currentDouble;

    /*
     * We use this to stash a value when clearAndGetNumber is called, until the next value is
     * reported
     */
    private Number lastValue = 0;

    private enum WHICH_TO_USE {
        NONE,
        LONG,
        DOUBLE
    }

    private WHICH_TO_USE which = WHICH_TO_USE.NONE;

    public synchronized void setValue(long value) {
        currentLong = value;
        which = WHICH_TO_USE.LONG;
    }

    public synchronized void setValue(double value) {
        currentDouble = value;
        which = WHICH_TO_USE.DOUBLE;
    }

    public synchronized void add(long value) {
        switch (which) {
        case NONE:
            setValue(value);
            break;
        case DOUBLE:
            currentDouble += value;
            break;
        case LONG:
            currentLong += value;
            break;
        default:
            throw new IllegalArgumentException("Unknown which value");
        }
    }

    public synchronized void add(double value) {
        switch (which) {
        case NONE:
            setValue(value);
            break;
        case LONG:
            which = WHICH_TO_USE.DOUBLE;
            currentDouble = currentLong + value;
            break;
        case DOUBLE:
            currentDouble += value;
            break;
        default:
            throw new IllegalArgumentException("Unknown which value");
        }
    }

    public synchronized Number clearAndGetNumber() {
        switch (which) {
        case NONE:
            return null;
        case DOUBLE:
            which = WHICH_TO_USE.NONE;
            lastValue = currentDouble;
            return Double.valueOf(currentDouble);
        case LONG:
            which = WHICH_TO_USE.NONE;
            lastValue = currentLong;
            return Long.valueOf(currentLong);
        }
        throw new IllegalArgumentException("Unknown which value");
    }

    public Number getNumber() {
        // N.B. synchronization not needed here since we're not making any modifications
        // in the worst case, we lose a race with clearAndGetNumber and get a slightly
        // out-of-date value
        switch (which) {
        case DOUBLE:
            return currentDouble;
        case LONG:
            return currentLong;
        case NONE:
        default:
            return lastValue;
        }
    }
}
