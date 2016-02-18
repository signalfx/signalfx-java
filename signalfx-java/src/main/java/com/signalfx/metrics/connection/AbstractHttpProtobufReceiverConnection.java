package com.signalfx.metrics.connection;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

import com.signalfx.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public abstract class AbstractHttpProtobufReceiverConnection extends AbstractHttpReceiverConnection implements DataPointEventReceiver {

    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");

    public AbstractHttpProtobufReceiverConnection (
            SignalFxReceiverEndpoint endpoint,
            int timeoutMs, HttpClientConnectionManager httpClientConnectionManager) {
       super(endpoint, timeoutMs, httpClientConnectionManager);
    }

    @Override
    public void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalFxMetricsException {
        if (dataPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth, getEntityForVersion(dataPoints),
                        getEndpointForAddDatapoints());
                checkHttpResponse(resp);
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to addDataPoints", e);
        }
    }

    @Override
    public void addEvents(String auth, List<SignalFxProtocolBuffers.Event> events)
            throws SignalFxMetricsException {
        if (events.isEmpty()){
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth, getEntityForVersion(events),
                        getEndpointForAddEvents());
                checkHttpResponse(resp);
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to addDataPoints", e);
        }
    }

    public void checkHttpResponse(CloseableHttpResponse resp) throws SignalFxMetricsException {
        final String body;
        try {
            body = IOUtils.toString(resp.getEntity().getContent());
        } catch (IOException e) {
            throw new SignalFxMetricsException("Unable to get response content", e);
        }
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new SignalFxMetricsException("Invalid status code "
                    + resp.getStatusLine().getStatusCode() + ": " + body);
        }
        if (!"\"OK\"".equals(body)) {
            throw new SignalFxMetricsException("Invalid response body: " + body);
        }
    }

    protected abstract String getEndpointForAddDatapoints();

    protected abstract String getEndpointForAddEvents();

    protected abstract HttpEntity getDataPointsEntityForVersion(
            List<SignalFxProtocolBuffers.DataPoint> dataPoints);

    protected abstract HttpEntity getEventsEntityForVersion(
            List<SignalFxProtocolBuffers.Event> events);

    @Override
    public void backfillDataPoints(String auth, String source, String metric,
                                   List<SignalFxProtocolBuffers.Datum> datumPoints)
            throws SignalFxMetricsException {
        if (datumPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new InputStreamEntity(
                                new ProtocolBufferStreamingInputStream<SignalFxProtocolBuffers.Datum>(
                                        datumPoints.iterator()), PROTO_TYPE)
                        , "/v1/backfill");
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalFxMetricsException(
                            "Invalid status coded " + resp.getStatusLine().getStatusCode());
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to addDataPoints", e);
        }
    }
}
