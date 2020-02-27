package com.splunk.signalfx;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.github.os72.protobuf351.InvalidProtocolBufferException;
import com.google.common.collect.ImmutableMap;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class ProtobufEncoderTest {

    @Test
    public void encode() throws InvalidProtocolBufferException {
        ProtobufEncoder e = new ProtobufEncoder();
        IntPoint pt = new IntPoint("some.metric", ImmutableMap.of("key", "val"), MetricType.COUNTER, 1000, 42);
        byte[] bytes = e.encode(Collections.singletonList(pt));
        SignalFxProtocolBuffers.DataPointUploadMessage protoMsg = SignalFxProtocolBuffers.DataPointUploadMessage.parseFrom(bytes);
        SignalFxProtocolBuffers.DataPoint protoPt = protoMsg.getDatapoints(0);
        assertEquals(pt.getValue(), protoPt.getValue().getIntValue());
        assertEquals(pt.getMetric(), protoPt.getMetric());
        SignalFxProtocolBuffers.Dimension protoDim = protoPt.getDimensions(0);
        Map.Entry<String, String> dim = pt.getDimensions().entrySet().iterator().next();
        assertEquals(dim.getKey(), protoDim.getKey());
        assertEquals(dim.getValue(), protoDim.getValue());
        assertEquals(pt.getType().ordinal(), protoPt.getMetricType().ordinal());
        assertEquals(pt.getTimestamp(), protoPt.getTimestamp());
    }
}
