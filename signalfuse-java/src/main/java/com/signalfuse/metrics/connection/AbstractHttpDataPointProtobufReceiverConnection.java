package com.signalfuse.metrics.connection;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

import com.signalfuse.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfuse.connection.AbstractHttpReceiverConnection;
import com.signalfuse.endpoint.SignalFuseReceiverEndpoint;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public abstract class AbstractHttpDataPointProtobufReceiverConnection extends AbstractHttpReceiverConnection implements DataPointReceiver {

    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");

    public AbstractHttpDataPointProtobufReceiverConnection(
            SignalFuseReceiverEndpoint endpoint,
            int timeoutMs, HttpClientConnectionManager httpClientConnectionManager) {
       super(endpoint, timeoutMs, httpClientConnectionManager);
    }

    @Override
    public void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException {
        if (dataPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth, getEntityForVersion(dataPoints),
                        getEndpointForAddDatapoints());
                final String body;
                try {
                    body = IOUtils.toString(resp.getEntity().getContent());
                } catch (IOException e) {
                    throw new SignalfuseMetricsException("Unable to get reponse content", e);
                }
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalfuseMetricsException("Invalid status code "
                            + resp.getStatusLine().getStatusCode() + ": " + body);
                }
                if (!"\"OK\"".equals(body)) {
                    throw new SignalfuseMetricsException("Invalid response body: " + body);
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalfuseMetricsException("Exception posting to addDataPoints", e);
        }
    }

    protected abstract String getEndpointForAddDatapoints();

    protected abstract HttpEntity getEntityForVersion(
            List<SignalFuseProtocolBuffers.DataPoint> dataPoints);

    @Override
    public void backfillDataPoints(String auth, String source, String metric,
                                   List<SignalFuseProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException {
        if (datumPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new InputStreamEntity(
                                new ProtocolBufferStreamingInputStream<SignalFuseProtocolBuffers.Datum>(
                                        datumPoints.iterator()), PROTO_TYPE)
                        , "/v1/backfill");
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalfuseMetricsException(
                            "Invalid status coded " + resp.getStatusLine().getStatusCode());
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalfuseMetricsException("Exception posting to addDataPoints", e);
        }
    }
}
