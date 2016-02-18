package com.signalfx.metrics.connection;

public class StaticDataPointEventReceiverFactory implements DataPointEventReceiverFactory {
    private final  DataPointEventReceiver dataPointEventReciever;

    public StaticDataPointEventReceiverFactory(DataPointEventReceiver dataPointEventReciever) {
        this.dataPointEventReciever = dataPointEventReciever;
    }

    @Override public DataPointEventReceiver createDataPointEventReceiver() {
        return this.dataPointEventReciever;
    }
}
