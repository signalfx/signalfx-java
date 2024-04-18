package com.signalfx.metrics.connection;

import java.io.IOException;
import java.util.List;

import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

public abstract class AbstractHttpEventProtobufReceiverConnection extends AbstractHttpReceiverConnection implements EventReceiver {

    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");

    public AbstractHttpEventProtobufReceiverConnection(
            SignalFxReceiverEndpoint endpoint,
            int timeoutMs, HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
    }

    public AbstractHttpEventProtobufReceiverConnection(
            SignalFxReceiverEndpoint endpoint,
            int timeoutMs, int maxRetries, HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, maxRetries, httpClientConnectionManager);
    }

    @Override
    public void addEvents(String auth, List<SignalFxProtocolBuffers.Event> events)
            throws SignalFxMetricsException {
        if (events.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        getEntityForVersion(events),
                        getEndpointForAddEvents(),
                        false);
                checkHttpResponse(resp);
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to addEvents", e);
        }
    }

    protected abstract String getEndpointForAddEvents();

    protected abstract HttpEntity getEntityForVersion(
            List<SignalFxProtocolBuffers.Event> events);
}
