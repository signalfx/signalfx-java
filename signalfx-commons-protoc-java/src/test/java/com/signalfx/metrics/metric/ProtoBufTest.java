package com.signalfx.metrics.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.github.os72.protobuf351.InvalidProtocolBufferException;
import com.signalfx.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.DataPoint;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.Datum;
import com.google.common.base.Preconditions;

/**
 * Simple protobuf test that shows protobufs are compiled and showing how to encode/decode them
 * @author jack
 */
@SuppressWarnings("MagicNumber")
public class ProtoBufTest {

    @Test
    public void testProtoBuilders() throws InvalidProtocolBufferException {
        Datum D = Datum.newBuilder().setDoubleValue(1.1).build();
        byte[] encoded = D.toByteArray();
        Datum decoded = Datum.parseFrom(encoded);
        assertEquals(decoded, D);
        assertNotEquals(Datum.getDefaultInstance(), D);
        assertEquals(1.1, D.getDoubleValue(), .0001);
    }

    @Test
    public void testProtoStreaming() throws IOException {
        List<DataPoint> pointsToWrite = new ArrayList<DataPoint>();
        pointsToWrite.add(DataPoint.newBuilder().setSource("tests").setMetric("testm").
                setTimestamp(1234).setValue(Datum.newBuilder().setIntValue(12)).build());
        pointsToWrite.add(DataPoint.newBuilder().setSource("tests").setMetric("testm").
                setTimestamp(1235).setValue(Datum.newBuilder().setIntValue(13)).build());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (DataPoint dp: pointsToWrite) {
            dp.writeDelimitedTo(bout);
        }

        byte[] rawBytes = IOUtils.toByteArray(new ByteArrayInputStream(bout.toByteArray()));
        byte[] smartBytes = IOUtils.toByteArray(new ProtocolBufferStreamingInputStream<DataPoint>(pointsToWrite.iterator()));
        Preconditions.checkArgument(Arrays.equals(rawBytes, smartBytes));

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());

        verifyInputStream(pointsToWrite, bin);
        verifyInputStream(pointsToWrite, new ByteArrayInputStream(smartBytes));

        verifyInputStream(pointsToWrite, new ProtocolBufferStreamingInputStream<DataPoint>(pointsToWrite.iterator()));
    }

    private static void verifyInputStream(List<DataPoint> originalPoints, InputStream in) throws IOException {
        List<DataPoint> pointsRead = new ArrayList<DataPoint>();
        while (true) {
            DataPoint dp = DataPoint.parseDelimitedFrom(in);
            if (dp == null) {
                break;
            } else {
                pointsRead.add(dp);
            }
        }
        assertEquals(originalPoints, pointsRead);
    }
}
