package com.signalfx.metrics.connection;

import java.util.List;

import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public interface EventReceiver {
    void addEvents(String auth, List<SignalFxProtocolBuffers.Event> events)
            throws SignalFxMetricsException;
}
