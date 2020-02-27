package com.splunk.signalfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * Encodes datapoints to a ProtocolBuffers format appropriate for SignalFx ingest.
 */
class ProtobufEncoder implements Encoder {

    @Override
    public String getType() {
        return "application/x-protobuf";
    }

    @Override
    public byte[] encode(Iterable<Point> points) {
        List<SignalFxProtocolBuffers.DataPoint> protoPts = new ArrayList<>();
        for (Point pt : points) {
            SignalFxProtocolBuffers.DataPoint.Builder builder = SignalFxProtocolBuffers.DataPoint.newBuilder();
            builder.setMetric(pt.getMetric());
            addDimensions(builder, pt);
            int ordinal = pt.getType().ordinal();
            SignalFxProtocolBuffers.MetricType protobufType = SignalFxProtocolBuffers.MetricType.values()[ordinal];
            builder.setMetricType(protobufType);
            if (pt.getTimestamp() != 0) {
                builder.setTimestamp(pt.getTimestamp());
            }
            builder.setValue(mkValue(pt));
            protoPts.add(builder.build());
        }
        return SignalFxProtocolBuffers.DataPointUploadMessage.newBuilder().addAllDatapoints(protoPts).build().toByteArray();
    }

    private static SignalFxProtocolBuffers.Datum.Builder mkValue(Point pt) {
        SignalFxProtocolBuffers.Datum.Builder value = SignalFxProtocolBuffers.Datum.newBuilder();
        if (pt instanceof IntPoint) {
            value.setIntValue(((IntPoint) pt).getValue());
        } else {
            value.setDoubleValue(((DoublePoint) pt).getValue());
        }
        return value;
    }

    private static void addDimensions(SignalFxProtocolBuffers.DataPoint.Builder builder, Point pt) {
        for (Map.Entry<String, String> dim : pt.getDimensions().entrySet()) {
            SignalFxProtocolBuffers.Dimension.Builder dimBuilder = SignalFxProtocolBuffers.Dimension.newBuilder();
            dimBuilder.setKey(dim.getKey());
            dimBuilder.setValue(dim.getValue());
            builder.addDimensions(dimBuilder.build());
        }
    }
}
