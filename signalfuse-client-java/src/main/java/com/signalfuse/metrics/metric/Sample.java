package com.signalfuse.metrics.metric;

/**
 * A sample is a metric that represents a duration of time for some distinct value.
 * 
 * Use a latency gauge and Java's autoclosable to time a method. {@code
 * try (Sample.Timer G =  metricFactory.createSample("latency").time()) {
 * try {
 * Thread.sleep(1000);
 * } catch (InterruptedException e) {
 * e.printStackTrace();
 * }
 * }
 * }
 * 
 * @author jack
 */
public interface Sample extends Metric {
    Timer time();

    void addSample(double value);

    void addSample(long value);

    interface Timer extends AutoCloseable {
        @Override void close();
    }
}
