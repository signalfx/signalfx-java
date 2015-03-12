package com.signalfx.metrics.connection;

public class StaticDataPointReceiverFactory implements DataPointReceiverFactory {
    private final DataPointReceiver dataPointReciever;

    public StaticDataPointReceiverFactory(DataPointReceiver dataPointReciever) {
        this.dataPointReciever = dataPointReciever;
    }

    @Override public DataPointReceiver createDataPointReceiver() {
        return this.dataPointReciever;
    }
}
